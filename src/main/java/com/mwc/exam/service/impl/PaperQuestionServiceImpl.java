package com.mwc.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.entity.PaperQuestion;
import com.mwc.exam.mapper.PaperQuestionMapper;
import com.mwc.exam.service.PaperQuestionService;
import org.springframework.stereotype.Service;

@Service
public class PaperQuestionServiceImpl extends ServiceImpl<PaperQuestionMapper, PaperQuestion> implements PaperQuestionService {
}
