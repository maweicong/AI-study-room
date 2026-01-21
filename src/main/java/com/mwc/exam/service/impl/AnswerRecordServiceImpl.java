package com.mwc.exam.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.entity.AnswerRecord;
import com.mwc.exam.mapper.AnswerRecordMapper;
import com.mwc.exam.service.AnswerRecordService;
import org.springframework.stereotype.Service;

@Service
public class AnswerRecordServiceImpl extends ServiceImpl<AnswerRecordMapper, AnswerRecord> implements AnswerRecordService {
}
