package com.mwc.exam.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.mwc.exam.entity.ExamRecord;
import com.mwc.exam.entity.Paper;
import com.mwc.exam.entity.PaperQuestion;
import com.mwc.exam.entity.Question;
import com.mwc.exam.mapper.ExamRecordMapper;
import com.mwc.exam.mapper.PaperMapper;
import com.mwc.exam.mapper.QuestionMapper;
import com.mwc.exam.service.PaperQuestionService;
import com.mwc.exam.service.PaperService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.service.QuestionService;
import com.mwc.exam.vo.AiPaperVo;
import com.mwc.exam.vo.PaperVo;
import com.mwc.exam.vo.RuleVo;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 试卷服务实现类
 */
@Slf4j
@Service
public class PaperServiceImpl extends ServiceImpl<PaperMapper, Paper> implements PaperService {

@Autowired
private PaperQuestionService paperQuestionService;
@Autowired
private QuestionService questionService;
@Autowired
private QuestionMapper questionMapper;
@Autowired
private ExamRecordMapper examRecordMapper;
    @Override
    public List<Paper> listPapers(String name, String status) {
        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!ObjectUtils.isEmpty(name), Paper::getName, name);
        queryWrapper.eq(!ObjectUtils.isEmpty(status), Paper::getStatus, status);
        queryWrapper.orderByDesc(Paper::getCreateTime);
        return list(queryWrapper);
    }
    /*
    * 手动创建试卷
    * */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Paper createPaper(PaperVo paperVo) {
        Paper paper = new Paper();
        BeanUtils.copyProperties(paperVo, paper);
        paper.setStatus("DRAFT");
        //判断是否有题目
        if(ObjectUtils.isEmpty(paperVo.getQuestions())){
            paper.setTotalScore(BigDecimal.ZERO);
            paper.setQuestionCount(0);
            save(paper);
            log.warn("当前试卷{}没有选择题目，只能用试卷编辑，不能用于考试", paper.getName());
            return paper;
        }
        //有题目根据题目计算试卷总分和题目数量
        //设置总题目数量
        paper.setQuestionCount(paperVo.getQuestions().size());
        //设置总分
        int sum = paperVo.getQuestions().values().stream().mapToInt(BigDecimal::intValue).sum();
        paper.setTotalScore(new BigDecimal(sum));
        save(paper);
        log.info("创建试卷成功，试卷ID：{}", paper.getId());
        //将题目集合添加到试卷题目中间表
        paperVo.getQuestions().forEach((questionId, score) -> {
            log.info("添加试卷题目关系，试卷ID：{}，题目ID：{}，分数：{}", paper.getId(), questionId, score);
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(Math.toIntExact(paper.getId()));
            paperQuestion.setQuestionId((long) Math.toIntExact(questionId));
            paperQuestion.setScore(score);
            paperQuestionService.save(paperQuestion);
        });
        return paper;
    }

    @Override
    public Paper createPaperWithAI(AiPaperVo aiPaperVo) {
        //保存试卷基本信息
        Paper paper = new Paper();
        BeanUtils.copyProperties(aiPaperVo, paper);
        paper.setStatus("DRAFT");
        save(paper);
        log.info("创建试卷成功，试卷ID：{}", paper.getId());
        //定义总分数和总题目数量
        int paperQuestionCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;
        //循环每个规则
        for (RuleVo ruleVo : aiPaperVo.getRules()) {
            log.info("开始处理规则：{}", ruleVo);
            if (ruleVo.getCount()==0){
                log.warn("当前规则没有题目，跳过");
                continue;
            }
            //设置条件
            LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(!ObjectUtils.isEmpty(ruleVo.getType()),Question::getType, ruleVo.getType());
            queryWrapper.in(!ObjectUtils.isEmpty(ruleVo.getCategoryIds()),Question::getCategoryId, ruleVo.getCategoryIds());
            //查询符合条件的题目
            List<Question> questionAllList = questionMapper.selectList(queryWrapper);
            //校验，可能满足规则的题目集合为null,直接跳出循环
            if (ObjectUtils.isEmpty(questionAllList)){
                log.warn("在：{}规则下没有题目，跳过",ruleVo.getType().name());
                continue;
            }
            //校验，检查满足满足规则题目的数量和规则要的数量谁小，取小
            int realCount = Math.min(ruleVo.getCount(), questionAllList.size());
            //计算总题目数量
            paperQuestionCount += realCount;
            //计算分数
            totalScore = totalScore.add(new BigDecimal(realCount * ruleVo.getScore()));
            //随机选出符合条件的题目集合
            Collections.shuffle(questionAllList);
            List<Question> questionList = questionAllList.stream().limit(realCount).collect(Collectors.toList());
            //将题目集合转成PaperQuestion
             questionList.forEach(question -> {
                 PaperQuestion paperQuestion = new PaperQuestion();
                 paperQuestion.setPaperId(Math.toIntExact(paper.getId()));
                 paperQuestion.setQuestionId(question.getId());
                 paperQuestion.setScore(new BigDecimal(ruleVo.getScore()));
                 paperQuestionService.save(paperQuestion);
             });
        }
        paper.setTotalScore(totalScore);
        paper.setQuestionCount(paperQuestionCount);
        updateById(paper);
        return paper;
    }

    @Override
    public Paper updatePaper(Integer id, PaperVo paperVo) {
        Paper paper = getById(id);
        //校验
        if (paper.getStatus().equals("PUBLISHED")){
            throw new RuntimeException("当前试卷已发布，不能修改");
        }
        //更新后的试卷名字不能重复
        LambdaQueryWrapper<Paper> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Paper::getId, id);
        queryWrapper.eq(Paper::getName, paperVo.getName());
        if (count(queryWrapper) > 0){
            throw new RuntimeException("当前试卷名字已存在");
        }
        BeanUtils.copyProperties(paperVo, paper);
        updateById(paper);
        log.info("更新试卷成功，试卷ID：{}", paper.getId());
        //删除原来的题目
        if (!ObjectUtils.isEmpty(paperVo.getQuestions())){
            paperQuestionService.remove(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, id));
        }
        //更新题目数量
        paper.setQuestionCount(paperVo.getQuestions().size());
        //更新总分数
        int sum = paperVo.getQuestions().values().stream().mapToInt(BigDecimal::intValue).sum();
        paper.setTotalScore(new BigDecimal(sum));
        //更新试卷题目
        paperVo.getQuestions().forEach((questionId, score) -> {
            log.info("添加试卷题目关系，试卷ID：{}，题目ID：{}，分数：{}", paper.getId(), questionId, score);
            PaperQuestion paperQuestion = new PaperQuestion();
            paperQuestion.setPaperId(Math.toIntExact(paper.getId()));
            paperQuestion.setQuestionId((long) Math.toIntExact(questionId));
            paperQuestion.setScore(score);
            paperQuestionService.save(paperQuestion);
            log.info("添加成功");
        });
        return paper;
    }

    @Override
    public void updatePaperStatus(Integer id, String status) {
        LambdaUpdateWrapper<Paper> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Paper::getId, id);
        updateWrapper.set(Paper::getStatus, status);
        update(updateWrapper);
        log.info("更新试卷状态成功，试卷ID：{}，状态：{}", id, status);
    }

    @Override
    public void removePaperById(Integer id) {
        Paper paper = getById(id);
        if (paper.getStatus().equals("PUBLISHED")){
            throw new RuntimeException("当前试卷已发布，不能删除");
        }
        //校验考试中是否有人用试卷
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamRecord::getExamId, id);
        if (examRecordMapper.selectCount(queryWrapper) > 0){
            throw new RuntimeException("当前试卷有考试在使用，不能删除");
        }
        removeById(id);
        //删除试卷题目
        paperQuestionService.remove(new LambdaQueryWrapper<PaperQuestion>().eq(PaperQuestion::getPaperId, id));
         log.info("删除试卷成功，试卷ID：{}", id);
    }

    @Override
    public Paper getByPaperId(Integer id) {
        Paper paper = getById(id);
        List<Question> paperQuestions = questionMapper.getPaperQuestions(id);
        //校验：试卷对应的题目是否为空
        if(ObjectUtils.isEmpty(paperQuestions)){
            log.warn("试卷ID：{}对应的题目为空", id);
            return paper;
        }
        //对题目集合进行排序，根据type类型排序
        paperQuestions.sort((o1, o2) -> Integer.compare(typeToInt(o1.getType()), typeToInt(o2.getType())));
        //题目集合赋值给试卷对象
        paper.setQuestions(paperQuestions);
        return paper;
    }
    private int typeToInt(String  type){
        return switch (type) {
            case "CHOICE" -> 1;
            case "JUDGE" -> 2;
            case "TEXT" -> 3;
            default -> 4;
        };
    }


}