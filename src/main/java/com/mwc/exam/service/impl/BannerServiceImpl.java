package com.mwc.exam.service.impl;

import com.mwc.exam.entity.Banner;
import com.mwc.exam.mapper.BannerMapper;
import com.mwc.exam.service.BannerService;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mwc.exam.service.FileUploadService;
import io.minio.errors.*;
import io.netty.util.internal.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 轮播图服务实现类
 */
@Slf4j
@Service
public class BannerServiceImpl extends ServiceImpl<BannerMapper, Banner> implements BannerService {
    @Autowired
    private FileUploadService fileUploadService;

    @Override
    public String uploadBannerImage(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (file.isEmpty()){
            throw new RuntimeException("上传文件不能为空");
        }
        String contentType = file.getContentType();
        if(ObjectUtils.isEmpty(contentType) || !contentType.startsWith("image")){
            throw new RuntimeException("上传文件格式错误");
        }
        if (file.getSize() > 5 * 1024 * 1024){
            throw new RuntimeException("上传文件大小不能超过5M");
        }
        String uploadFile = fileUploadService.uploadFile("banners/", file);

        log.info("上传图片地址：{}", uploadFile);
        return uploadFile;
    }

    @Override
    public void addBanner(Banner banner) {
        //1.确认banner createTime和updateTime有时间
        //方式1：数据库设置时间  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
        //方案2：代码时间赋值   set new Date();
        //方案3：使用mybatis-plus自动填充功能 [知识点中会说明]
        //2.判断下启动状态
        if (banner.getIsActive() == null){
            banner.setIsActive(true);
        }
        //3.判断优先级
        if (banner.getSortOrder() == null){
            banner.setSortOrder(0);
        }
        //4.进行保存
        boolean isSuccess = save(banner);

        if (!isSuccess) {
            throw new RuntimeException("轮播图保存失败！");
        }

        log.info("轮播图保存成功！！");
    }
}