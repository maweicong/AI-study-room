package com.mwc.exam.mapper;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mwc.exam.entity.Question;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mwc.exam.vo.QuestionQueryVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 题目Mapper接口
 * 继承MyBatis Plus的BaseMapper，提供基础的CRUD操作
 */
public interface QuestionMapper extends BaseMapper<Question> {
    //获取所有题目的分类统计数据
    @Select("SELECT category_id AS categoryId, COUNT(*) AS count FROM questions where is_deleted=0 GROUP BY category_id")
    List<Map<String, Long>> getCategoryCount();
    //分页查询题目
    IPage<Question> selectQuestions(Page<Question> querypage, QuestionQueryVo queryVo);
    //获取试卷详情
    List<Question> getPaperQuestions(Integer paperId);
}