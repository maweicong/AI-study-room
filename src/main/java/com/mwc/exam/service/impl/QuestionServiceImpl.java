package com.mwc.exam.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mwc.exam.common.CacheConstants;
import com.mwc.exam.entity.*;
import com.mwc.exam.mapper.PaperQuestionMapper;
import com.mwc.exam.mapper.QuestionAnswerMapper;
import com.mwc.exam.mapper.QuestionChoiceMapper;
import com.mwc.exam.mapper.QuestionMapper;
import com.mwc.exam.service.KimiAiService;
import com.mwc.exam.service.QuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.utils.ExcelUtil;
import com.mwc.exam.utils.RedisUtils;
import com.mwc.exam.vo.AiGenerateRequestVo;
import com.mwc.exam.vo.QuestionImportVo;
import com.mwc.exam.vo.QuestionQueryVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题目Service实现类
 * 实现题目相关的业务逻辑
 */
@Slf4j
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question> implements QuestionService {
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;
    @Autowired
    private QuestionAnswerMapper questionAnswerMapper;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private PaperQuestionMapper paperQuestionMapper;
    @Autowired
    private KimiAiService kimiAiService;

    //方案二：嵌套查询
    @Override
    public void selectQuestions(Page<Question> querypage, QuestionQueryVo queryVo) {
         questionMapper.selectQuestions(querypage, queryVo);
    }
    //方案三：使用stream流
    @Override
    public void selectQuestionsByStream(Page<Question> querypage, QuestionQueryVo queryVo) {
        //查询所有题目（分页＋多条件）
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(queryVo.getCategoryId()!= null ,Question::getCategoryId, queryVo.getCategoryId());
        queryWrapper.eq(!ObjectUtils.isEmpty(queryVo.getDifficulty()), Question::getDifficulty, queryVo.getDifficulty());
        queryWrapper.eq(!ObjectUtils.isEmpty(queryVo.getType()), Question::getType, queryVo.getType());
        queryWrapper.like(!ObjectUtils.isEmpty(queryVo.getKeyword()), Question::getTitle, queryVo.getKeyword());
        queryWrapper.orderByDesc(Question::getCreateTime);
        page(querypage, queryWrapper);
        //如果没有满足上面条件的结果，后续步骤不用进行了
        if(ObjectUtils.isEmpty(querypage.getRecords())){
            return;
        }
        //根据题目ID查询题目选项
        fullAnswerAndChoice(querypage.getRecords());
    }

    private void fullAnswerAndChoice(List<Question> questionList) {
        //获取所有题目id列表
        List<Long> questionIds = questionList.stream().map(Question::getId).collect(Collectors.toList());
        //获取所有答案
        LambdaQueryWrapper<QuestionAnswer> answerQueryWrapper = new LambdaQueryWrapper<>();
        answerQueryWrapper.in(QuestionAnswer::getQuestionId, questionIds);
        List<QuestionAnswer> questionAnswers = questionAnswerMapper.selectList(answerQueryWrapper);
        //获取所有选项
        LambdaQueryWrapper<QuestionChoice> choiceQueryWrapper = new LambdaQueryWrapper<>();
        choiceQueryWrapper.in(QuestionChoice::getQuestionId, questionIds);
        List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(choiceQueryWrapper);
        //将答案集合转成Map
        Map<Long, QuestionAnswer> answerMap = questionAnswers.stream().collect(Collectors.toMap(QuestionAnswer::getQuestionId, a -> a));
        //将选项根据id值进行分组
        Map<Long,  List<QuestionChoice>> choiceMap = questionChoices.stream().collect(Collectors.groupingBy(QuestionChoice::getQuestionId));
        //循环题目列表，设置答案和选项
        for (Question question : questionList) {
            question.setAnswer(answerMap.get(question.getId()));
            //只有选择题有选项
            if (question.getType().equals("CHOICE")){
                //!!!!!!!!!!!!!!!!考虑选项排序问题
                List<QuestionChoice> questionChoices1 = choiceMap.get(question.getId());
                if (!ObjectUtils.isEmpty(questionChoices1)){
                    //从小到大正序
                    questionChoices1.sort(Comparator.comparing(QuestionChoice::getSort));
                    question.setChoices(choiceMap.get(question.getId()));
                }
            }
        }
    }

    @Override
    public Question selectQuestionsById(Long id) {
        //查询题目详情
        Question question = getById(id);
        //条件判断
        if (question == null) {
            throw new RuntimeException("题目不存在");
        }
        //查询题目对应答案
        QuestionAnswer questionAnswer = questionAnswerMapper.selectOne(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId, id));
        question.setAnswer(questionAnswer);
        //查询题目对应选项
        if (question.getType().equals("CHOICE")){
            List<QuestionChoice> questionChoices = questionChoiceMapper.selectList(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId, id));
            question.setChoices(questionChoices);
        }
        //进行redis缓存zset
        // 创建并启动一个新线程来执行分数增加操作
        new Thread(() -> increaseScore(question.getId())).start();

        return question;
    }
    /**
     * 创建题目
     * @param question 题目对象，包含题目的基本信息、答案和选项等
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createQuestion(Question question) {
        // 检查同一类型下是否已存在相同标题的题目，避免重复创建
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getType, question.getType());
        queryWrapper.eq(Question::getTitle, question.getTitle());
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("该类型下已存在该题目");
        }
        //保存题目信息
        save(question);
        //判断是不是选择题，如果是 根据选项的正确给答案赋值 同时将选项插入选项表
        QuestionAnswer questionAnswer = question.getAnswer();//如果是选择题答案为空
        questionAnswer.setQuestionId(question.getId());
        if (question.getType().equals("CHOICE")) {
            List<QuestionChoice> choices = question.getChoices();
            StringBuilder correctAnswer = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice questionChoice = choices.get(i);
                questionChoice.setQuestionId(question.getId());
                questionChoice.setSort(i);
                questionChoiceMapper.insert(questionChoice);
                if (questionChoice.getIsCorrect()){
                    if (correctAnswer.length() > 0){
                        correctAnswer.append(",");
                    }
                    correctAnswer.append((char)('A'+ i));
                }
                //通过选项给答案赋值
                questionAnswer.setAnswer(correctAnswer.toString());
            }
           //完成答案数据插入
            questionAnswerMapper.insert(questionAnswer);
        }
    }
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateQuestion(Question question) {
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getTitle, question.getTitle());
        queryWrapper.ne(Question::getId, question.getId());
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("该类型下已存在该题目");
        }
        //进行题目信息更新
        updateById(question);
        QuestionAnswer questionAnswer = question.getAnswer();
        //判断是不是选择题，如果是 根据选项的正确给答案赋值 同时将选项插入选项表
        if (question.getType().equals("CHOICE")){
            List<QuestionChoice> choices = question.getChoices();
           //删除旧选项
            LambdaQueryWrapper<QuestionChoice> choiceQueryWrapper = new LambdaQueryWrapper<>();
            choiceQueryWrapper.eq(QuestionChoice::getQuestionId, question.getId());
            questionChoiceMapper.delete(choiceQueryWrapper);
            //接受新答案
            StringBuilder correctAnswer = new StringBuilder();
            for (int i = 0; i < choices.size(); i++) {
                QuestionChoice questionChoice = choices.get(i);
                questionChoice.setId(null);
                questionChoice.setCreateTime( null);
                questionChoice.setUpdateTime(null);
                questionChoice.setQuestionId(question.getId());
                questionChoice.setSort(i);
                questionChoiceMapper.insert(questionChoice);
                if (questionChoice.getIsCorrect()){
                    if (correctAnswer.length() > 0){
                        correctAnswer.append(",");
                    }
                    correctAnswer.append((char)('A'+ i));
                }
            }
            questionAnswer.setAnswer(correctAnswer.toString());
        }
        questionAnswerMapper.updateById(questionAnswer);
    }

    @Override
    public void deleteQuestion(Long id) {
        //先判断是否有关联试卷
        LambdaQueryWrapper<PaperQuestion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PaperQuestion::getQuestionId, id);
        if (paperQuestionMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("该题目有关联试卷，请先解除关联");
        }
        //删除题目
        removeById(id);
        //删除关联的答案
        questionAnswerMapper.delete(new LambdaQueryWrapper<QuestionAnswer>().eq(QuestionAnswer::getQuestionId, id));
        //删除关联的选项
        questionChoiceMapper.delete(new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId, id));
    }

        /**
     * 查询热门题目列表
     *
     * @param size 需要查询的题目数量
     * @return 热门题目列表
     */
    @Override
    public List<Question> selectHotQuestions(Integer size) {
        //定义集合存储热门题目
        List<Question> hotQuestions = new ArrayList<>();

        //从缓存中获取热门题目 倒叙取值
        Set<Object> hotQuestion = redisUtils.zReverseRange(CacheConstants.POPULAR_QUESTIONS_KEY, 0, size - 1);

        //根据缓存中的题目ID查询题目详细信息
        if(!ObjectUtils.isEmpty(hotQuestion)){
            List<Long> hotIds = hotQuestion.stream().map(id -> Long.valueOf(id.toString())).collect(Collectors.toList());
            //有序查询题目信息
            for (Long id : hotIds){
                Question question =getById(id);
                //校验题目是否存在
                if(question!= null){
                    hotQuestions.add(question);
                }
            }
        }

        //检查热门题目是否满足size，不足则从数据库补充
        int diff = size - hotQuestions.size();
        if (diff > 0) {
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.orderByDesc(Question::getCreateTime);
            //已有id进行过滤
            List<Long> existIds = hotQuestions.stream().map(Question::getId).collect(Collectors.toList());
            if (!ObjectUtils.isEmpty(existIds)){
                queryWrapper.notIn(Question::getId, existIds);
            }
            //切割指定diff
            List<Question> newQuestions = list(queryWrapper.last("limit " + diff));
            hotQuestions.addAll(newQuestions);
        }

        //给题目附答案和选项
        fullAnswerAndChoice(hotQuestions);
        return hotQuestions;
    }
    //题目预览
    @Override
    public List<QuestionImportVo> previewExcel(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        //校验文件格式
        if (originalFilename == null || originalFilename.isEmpty()){
            throw new RuntimeException("请选择文件");
        }
        if ( !originalFilename.endsWith(".xlsx") && !originalFilename.endsWith(".xls")){
            throw new RuntimeException("文件格式错误");
        }
        //解析excel
        List<QuestionImportVo> questionImportVos = ExcelUtil.parseExcel(file);
        //返回数据
        return questionImportVos;
    }

    @Override
    public String importQuestions(List<QuestionImportVo> questions) {
        if (ObjectUtils.isEmpty( questions)){
            return "导入结束，此次传入数据为空";
        }
        //定义服务降级
        int successNumber=0;
        for (QuestionImportVo questionImportVo : questions){
            try {
                Question question = new Question();
                BeanUtils.copyProperties(questionImportVo, question);
                //判断是不是选择题
                if (question.getType().equals("CHOICE")){
                    //给选择题选项赋值
                    List<QuestionChoice> choices = new ArrayList<>(questionImportVo.getChoices().size());
                    for (QuestionImportVo.ChoiceImportDto choiceImportDto : questionImportVo.getChoices()) {
                        QuestionChoice questionChoice = new QuestionChoice();
                        BeanUtils.copyProperties(choiceImportDto, questionChoice);
                        choices.add(questionChoice);
                    }
                    //设置选项给question对象
                    question.setChoices(choices);
                }
                //设置答案给question对象
                QuestionAnswer questionAnswer = new QuestionAnswer();
                //如果是判断题，VO中是小写的，转换成大写
                if (question.getType().equals("JUDGE")){
                    questionAnswer.setAnswer(questionImportVo.getAnswer().toUpperCase());
                }else {
                    questionAnswer.setAnswer(questionImportVo.getAnswer());
                }
                questionAnswer.setKeywords(questionImportVo.getKeywords());
                question.setAnswer(questionAnswer);
                //保存题目
                createQuestion(question);
                successNumber++;
            } catch (Exception e) {
                log.error("题目导入失败：{}", e.getMessage());
            }
        }
        String message = "导入结束，成功导入" + successNumber + "道题目,一共%s道".formatted(questions.size());
        return message;
    }



    /*
    * 进行异步调用，进行题目加分
    * */
    private void increaseScore(Long questionId) {
        Double value = redisUtils.zIncrementScore(CacheConstants.POPULAR_QUESTIONS_KEY, questionId, 1);
        log.debug("题目id{} 分数增加成功，当前分数：{}", questionId, value);
    }
}