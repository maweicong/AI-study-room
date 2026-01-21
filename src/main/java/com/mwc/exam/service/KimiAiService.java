package com.mwc.exam.service;


import com.mwc.exam.entity.Question;
import com.mwc.exam.vo.AiGenerateRequestVo;
import com.mwc.exam.vo.GradingResult;
import com.mwc.exam.vo.QuestionImportVo;

import java.util.List;

/**
 * Kimi AI服务接口
 * 用于调用Kimi API生成题目
 */
/**
 * KimiAI服务接口
 * 提供与KimiAI相关的功能操作，包括构建提示词、调用API以及生成题目等功能
 */
public interface KimiAiService {
    /**
     * 构建AI请求提示词
     * 根据传入的请求对象生成对应的提示词字符串
     *
     * @param request AI生成请求对象，包含构建提示词所需的相关参数
     * @return 构建完成的提示词字符串
     */
    String buildPrompt(AiGenerateRequestVo request);

    /**
     * 调用Kimi API接口
     * 向Kimi服务发送请求并获取响应结果
     *
     * @param prompt 发送给AI的提示词内容
     * @return API调用返回的结果字符串
     * @throws InterruptedException 当线程被中断时抛出异常
     */
    String callKimiApi(String prompt) throws InterruptedException;

    /**
     * 通过AI生成题目列表
     * 根据请求参数调用AI服务生成相应的题目数据
     *
     * @param request AI生成请求对象，包含生成题目所需的相关参数
     * @return 包含生成题目的导入对象列表
     * @throws InterruptedException 当线程被中断时抛出异常
     */
    List<QuestionImportVo> generateQuestionsByAi(AiGenerateRequestVo request) throws InterruptedException;
    /**
     * 简答题ai提示词和方法
     *
     * 该方法根据给定的问题、用户答案和最大分数生成一个用于评分的提示语，
     * 通常用于AI评分系统中提供标准化的评分依据。
     *
     * @param question 问题对象，包含需要评分的具体题目内容
     * @param userAnswer 用户提交的答案字符串
     * @param maxScore 该题目的最大可获得分数
     * @return 返回构建好的评分提示语字符串，可用于后续的自动评分处理
     */
    String buildGradingPrompt(Question question, String userAnswer, Integer maxScore);
    //考试评语ai提示词和方法
    String buildSummaryPrompt(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount);
    /**
     * 使用ai,进行简答题判断
     * @param question
     * @param userAnswer
     * @param maxScore
     * @return
     */
    GradingResult gradingTextQuestion(Question question, String userAnswer, Integer maxScore) throws InterruptedException;
    /**
     * 生成ai评语
     * @param totalScore
     * @param maxScore
     * @param questionCount
     * @param correctCount
     * @return
     */
    String buildSummary(Integer totalScore, Integer maxScore, Integer questionCount, Integer correctCount) throws InterruptedException;
}
