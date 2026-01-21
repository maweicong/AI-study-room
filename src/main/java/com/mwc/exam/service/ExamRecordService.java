package com.mwc.exam.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mwc.exam.entity.ExamRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mwc.exam.vo.ExamRankingVO;


import java.util.List;

/**
 * 考试记录Service接口
 * 定义考试记录相关的业务方法
 */
public interface ExamRecordService extends IService<ExamRecord> {
    // 分页查询考试记录
    void examRecord(Page<ExamRecord> pageObj, String studentName, Integer status, String startDate, String endDate);
    // 获取考试记录排行榜
    List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit);
}