package com.mwc.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mwc.exam.entity.ExamRecord;
import com.mwc.exam.entity.Paper;
import com.mwc.exam.mapper.ExamRecordMapper;
import com.mwc.exam.service.ExamRecordService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.service.PaperService;
import com.mwc.exam.vo.ExamRankingVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 考试记录Service实现类
 * 实现考试记录相关的业务逻辑
 */
@Slf4j
@Service
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements ExamRecordService {
@Autowired
private PaperService paperService;
@Autowired
private ExamRecordMapper examRecordMapper;

    @Override
    public void examRecord(Page<ExamRecord> pageObj, String studentName, Integer status, String startDate, String endDate) {
        LambdaQueryWrapper<ExamRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(!ObjectUtils.isEmpty(studentName),ExamRecord::getStudentName, studentName);
        if (status != null){
            String statusStr =switch ( status){
                case 0 -> "进行中";
                case 1 -> "已完成";
                case 2 -> "已批阅";
                default -> null;
            };
            queryWrapper.eq(!ObjectUtils.isEmpty(statusStr),ExamRecord::getStatus, statusStr);
        }
        //大于等于开始时间
        queryWrapper.ge(!ObjectUtils.isEmpty(startDate),ExamRecord::getStartTime, startDate);
        //小于等于结束时间
        queryWrapper.le(!ObjectUtils.isEmpty(endDate),ExamRecord::getEndTime, endDate);
        Page<ExamRecord> pageRecords = page(pageObj, queryWrapper);
        //查看考试记录下所有考试对象
        if (pageRecords == null || pageRecords.getRecords() == null || pageRecords.getRecords().isEmpty()) {
            log.info("考试记录为空");
            return;
        }
        List<Integer> pageIds = pageRecords.getRecords().stream().map(ExamRecord::getExamId).collect(Collectors.toList());
        //java代码将考试对象赋值给考试记录对象
        List<Paper> paperList = paperService.listByIds(pageIds);
        Map<Long, Paper> paperListIds = paperList.stream().collect(Collectors.toMap(Paper::getId, paper -> paper));
        pageRecords.getRecords().forEach(examRecord -> examRecord.setPaper(paperListIds.get(examRecord.getExamId().longValue())));
    }

    @Override
    public List<ExamRankingVO> getExamRanking(Integer paperId, Integer limit) {
        List<ExamRankingVO> examRanking = examRecordMapper.getExamRanking(paperId, limit);
        return examRanking;
    }
}