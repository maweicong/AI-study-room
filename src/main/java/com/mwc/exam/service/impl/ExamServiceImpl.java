package com.mwc.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mwc.exam.entity.AnswerRecord;
import com.mwc.exam.entity.ExamRecord;
import com.mwc.exam.entity.Paper;
import com.mwc.exam.entity.Question;
import com.mwc.exam.mapper.AnswerRecordMapper;
import com.mwc.exam.mapper.ExamRecordMapper;
import com.mwc.exam.service.AnswerRecordService;
import com.mwc.exam.service.ExamService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.service.KimiAiService;
import com.mwc.exam.service.PaperService;
import com.mwc.exam.vo.GradingResult;
import com.mwc.exam.vo.StartExamVo;
import com.mwc.exam.vo.SubmitAnswerVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 考试服务实现类
 */
@Service
@Slf4j
public class ExamServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamService {
@Autowired
private ExamRecordMapper examRecordMapper;
@Autowired
private PaperService paperService;
@Autowired
private AnswerRecordMapper answerRecordMapper;
@Autowired
private AnswerRecordService answerRecordService;
@Autowired
private KimiAiService kimiAiService;
    @Override
    public ExamRecord startExam(StartExamVo startExamVo) {
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getStudentName, startExamVo.getStudentName());
        queryWrapper.eq(ExamRecord::getExamId, startExamVo.getPaperId());
        queryWrapper.eq(ExamRecord::getStatus, "进行中");
        ExamRecord existExamRecord = examRecordMapper.selectOne(queryWrapper);
        if (existExamRecord != null){
            log.debug("用户名字：{}已开始考试", startExamVo.getStudentName());
            return existExamRecord;
        }
        ExamRecord examRecord = new ExamRecord();
        examRecord.setExamId(startExamVo.getPaperId());
        examRecord.setStudentName(startExamVo.getStudentName());
        examRecord.setStatus("进行中");
        examRecord.setStartTime(LocalDateTime.now());
        examRecord.setWindowSwitches(0);
        examRecordMapper.insert(examRecord);
        return examRecord;
    }

    @Override
    public ExamRecord customGetExamRecordById(Integer id) {
        //宏观：获取考试记录，考试记录对应的试卷对象，获取考试记录对应的答题记录集合
        //注意： 答题记录和顺序和考试记录的顺序相同！
        //1. 获取考试记录详情
        ExamRecord examRecord = getById(id);
        if (examRecord == null) {
            throw new RuntimeException("开始考试的记录已经被删除！");
        }
        //2. 获取考试记录对应试卷对象详情 【试卷 题目 选项 和 答案】
        Paper paper = paperService.getByPaperId(examRecord.getExamId());
        if (paper == null) {
            throw new RuntimeException("当前考试记录的试卷被删除！获取考试记录详情失败！");
        }
        //3. 获取考试记录对应的答题记录集合
        LambdaQueryWrapper<AnswerRecord> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(AnswerRecord::getExamRecordId,id);
        List<AnswerRecord> answerRecords = answerRecordService.list(lambdaQueryWrapper);
        if (!ObjectUtils.isEmpty(answerRecords)){
            //[8,2,1,3,7,4] -> 题目id
            List<Long> questionIdList = paper.getQuestions().stream().map(Question::getId).collect(Collectors.toList());
            //[{questionId:1} -> 2 ,{questionId:2} -> 1 ,{questionId:3} -> 3,{questionId:4} ->5,{questionId:7} -> 4,{questionId:8} -> 0]
            answerRecords.sort((o1, o2) -> {
                int x = questionIdList.indexOf(o1.getQuestionId());
                int y = questionIdList.indexOf(o2.getQuestionId());
                return Integer.compare(o1.getQuestionId(),o2.getQuestionId());
            });
        }
        //4. 数据组装即可
        examRecord.setPaper(paper);
        examRecord.setAnswerRecords(answerRecords);
        return examRecord;
    }


    @Override
    public void submitAnswers(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException {
        if(!ObjectUtils.isEmpty(answers)){
            List<AnswerRecord> answerRecordList  = answers.stream().map(vo -> new AnswerRecord(examRecordId, vo.getQuestionId(), vo.getUserAnswer())).collect(Collectors.toList());
            //进行answer批量插入
            answerRecordService.saveBatch(answerRecordList);
        }
        //修改考试记录对象
        ExamRecord examRecord = getById(examRecordId);
        examRecord.setStatus("已完成");
        examRecord.setEndTime(LocalDateTime.now());
        updateById(examRecord);
        //调用判卷业务方法完成判卷
        graderExam(examRecordId);
    }

    //ai智能判题功能

    /**
     * AI判卷方法
     * @param examRecordId
     * @return
     */
    @Override
    public ExamRecord graderExam(Integer examRecordId) throws InterruptedException {
        //宏观：  获取考试记录相关的信息（考试记录对象 考试记录答题记录 考试对应试卷）
        //  进行循环判断（1.答题记录进行修改 2.总体提到总分数 总正确数量）  修改考试记录（状态 -》 已批阅  修改 -》 总分数）   进行ai评语生成（总正确的题目数量）
        //  修改考试记录表  返回考试记录对象
        //1.获取考试记录和相关的信息（试卷和答题记录）
        ExamRecord examRecord = customGetExamRecordById(examRecordId);
        Paper paper = examRecord.getPaper();
        if (paper == null){
            examRecord.setStatus("已批阅");
            examRecord.setAnswers("考试对应的试卷被删除！无法进行成绩判定！");
            updateById(examRecord);
            throw new RuntimeException("考试对应的试卷被删除！无法进行成绩判定！");
        }
        List<AnswerRecord> answerRecords = examRecord.getAnswerRecords();
        if (ObjectUtils.isEmpty(answerRecords)){
            //没有提交
            examRecord.setStatus("已批阅");
            examRecord.setScore(0);
            examRecord.setAnswers("没有提交记录！成绩为零！继续加油！");
            updateById(examRecord);
            return examRecord;
        }

        //2.进行循环的判卷（1.记录总分数 2.记录正确题目数量 3. 修改每个答题记录的状态（得分，是否正确 0 1 2 ，text-》ai评语））
        int correctNumber = 0 ; //正确题目数量
        int totalScore = 0; //总得分

        //报错继续！ 某个记录错了，后续还需要继续判卷
        //将正确题目转成map,方便每次判断获取正确答案
        Map<Long, Question> questionMap = paper.getQuestions().stream().collect(Collectors.toMap(Question::getId, q -> q));

        for (AnswerRecord answerRecord : answerRecords) {
            try {
                //1.先获取 答题记录对应的题目对象
                Question question = questionMap.get(answerRecord.getQuestionId().longValue());
                String systemAnswer = question.getAnswer().getAnswer();
                String userAnswer = answerRecord.getUserAnswer();
                if ("JUDGE".equalsIgnoreCase(question.getType())){
                    //true false
                    userAnswer = normalizeJudgeAnswer(userAnswer);
                }
                if (!"TEXT".equals(question.getType())) {
                    //2.判断题目类型(选择和判断直接判卷)
                    if (systemAnswer.equalsIgnoreCase(userAnswer)){
                        answerRecord.setIsCorrect(1); //正确
                        answerRecord.setScore(question.getPaperScore().intValue());
                    }else{
                        answerRecord.setIsCorrect(0); //正确
                        answerRecord.setScore(0);
                    }
                }else{
                    //3.简答题进行ai判断
                    //简答题
                    GradingResult result =
                            kimiAiService.gradingTextQuestion(question,userAnswer,question.getPaperScore().intValue());
                    //分
                    answerRecord.setScore(result.getScore());
                    //ai评价 正确  feedback  非正确 reason
                    //是否正确 （满分 1 0分 0 其余就是2）
                    if (result.getScore() == 0){
                        answerRecord.setIsCorrect(0);
                        answerRecord.setAiCorrection(result.getReason());
                    }else if (result.getScore() == question.getPaperScore().intValue()){
                        answerRecord.setIsCorrect(1);
                        answerRecord.setAiCorrection(result.getFeedback());
                    }else{
                        answerRecord.setIsCorrect(2);
                        answerRecord.setAiCorrection(result.getReason());
                    }
                }
            } catch (Exception e) {
                answerRecord.setScore(0);
                answerRecord.setIsCorrect(0);
                answerRecord.setAiCorrection("判题过程出错！");
            }
            //进行记录修改
            //进行总分数赋值
            totalScore += answerRecord.getScore();
            //正确题目数量累加
            if (answerRecord.getIsCorrect() == 1){
                correctNumber++;
            }
        }
        answerRecordService.updateBatchById(answerRecords);

        //进行ai生成评价，进行考试记录修改和完善
        String summary = kimiAiService.
                buildSummary(totalScore, paper.getTotalScore().intValue(), paper.getQuestionCount(), correctNumber);

        examRecord.setScore(totalScore);
        examRecord.setAnswers(summary);
        examRecord.setStatus("已批阅");
        updateById(examRecord);

        return examRecord;
    }

    @Override
    public void custRemoveById(Integer id) {
        ExamRecord record = getById(id);
        if(record.getStatus().equals("进行中")){
            throw new RuntimeException("进行中的考试不能删除！");
        }
        //删除自身
        removeById(id);
        //删除关联的答题记录
        answerRecordService.remove(new QueryWrapper<AnswerRecord>().eq("exam_record_id",id));
    }

    /**
     * 标准化判断题答案，将T/F转换为TRUE/FALSE
     * @param answer 原始答案
     * @return 标准化后的答案
     */
    private String normalizeJudgeAnswer(String answer) {
        if (answer == null || answer.trim().isEmpty()) {
            return "";
        }

        String normalized = answer.trim().toUpperCase();
        switch (normalized) {
            case "T":
            case "TRUE":
            case "正确":
                return "TRUE";
            case "F":
            case "FALSE":
            case "错":
                return "FALSE";
            default:
                return normalized;
        }
    }

}