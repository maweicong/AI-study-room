package com.mwc.exam.service;

import com.mwc.exam.entity.ExamRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mwc.exam.vo.StartExamVo;
import com.mwc.exam.vo.SubmitAnswerVo;

import java.util.List;

/**
 * 考试服务接口
 */
public interface ExamService extends IService<ExamRecord> {
    //开始考试
    ExamRecord startExam(StartExamVo startExamVo);
    //获取考试详情
    ExamRecord customGetExamRecordById(Integer id);
    //提交考试记录并进行判卷
    void submitAnswers(Integer examRecordId, List<SubmitAnswerVo> answers) throws InterruptedException;
    //判卷
    ExamRecord graderExam(Integer examRecordId) throws InterruptedException;
    //删除考试记录
    void custRemoveById(Integer id);
}
 