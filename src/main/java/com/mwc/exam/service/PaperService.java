package com.mwc.exam.service;

import com.mwc.exam.entity.Paper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mwc.exam.vo.AiPaperVo;
import com.mwc.exam.vo.PaperVo;

import java.util.List;

/**
 * 试卷服务接口
 */
public interface PaperService extends IService<Paper> {
    // 获取试卷列表
    List<Paper> listPapers(String name, String status);

    //手动创建试卷
    Paper createPaper(PaperVo paperVo);
    // 自动创建试卷
    Paper createPaperWithAI(AiPaperVo aiPaperVo);
    // 更新试卷
    Paper updatePaper(Integer id, PaperVo paperVo);
    // 更新试卷状态
    void updatePaperStatus(Integer id, String status);
    // 删除试卷
    void removePaperById(Integer id);
    // 获取试卷详情
    Paper getByPaperId(Integer id);

}