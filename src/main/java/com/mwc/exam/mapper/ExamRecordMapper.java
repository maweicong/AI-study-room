package com.mwc.exam.mapper;


import com.mwc.exam.entity.ExamRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mwc.exam.vo.ExamRankingVO;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @description 针对表【exam_record(考试记录表)】的数据库操作Mapper
 * @createDate 2025-06-20 22:37:43
 * @Entity com.atguigu.exam.entity.ExamRecord
 */
@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    List<ExamRankingVO> getExamRanking(@Param("paperId") Integer paperId, @Param("limit") Integer limit);
}