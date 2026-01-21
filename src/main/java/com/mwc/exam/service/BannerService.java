package com.mwc.exam.service;

import com.mwc.exam.entity.Banner;
import com.baomidou.mybatisplus.extension.service.IService;
import io.minio.errors.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * 轮播图服务接口
 */
public interface BannerService extends IService<Banner> {


    /*
    * 图片上传接口
    * @param file 图片文件
    * @return 图片访问URL
    * */
    String uploadBannerImage(MultipartFile file) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;
    /*
    * 添加轮播图
    * @param banner 轮播图对象
    * @return 操作结果
    * */

    void addBanner(Banner banner);
}