package com.mwc.exam.mapper;


import com.mwc.exam.entity.QuestionChoice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目选项
 */
public interface QuestionChoiceMapper extends BaseMapper<QuestionChoice> {
    @Select("select * from question_choices where is_deleted=0 and question_id = #{questionId} order by sort")
    List<QuestionChoice> selectListByQuestionId(Long questionId);


} 