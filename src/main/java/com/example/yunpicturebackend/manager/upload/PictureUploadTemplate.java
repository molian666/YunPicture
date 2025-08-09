package com.example.yunpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.example.yunpicturebackend.config.CosClientConfig;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.exception.ThrowUtils;
import com.example.yunpicturebackend.manager.CosManager;
import com.example.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板
 */
@Component
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param inputSource
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //校验图片
        validPicture(inputSource);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
        //自己拼接文件上传路径，提高安全性
        String uplaodFileName = String.format("%s %s %s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uplaodFileName);
        //解析结果
        File tempFile = null;
        try {
            //创建临时文件,获取文件到服务器
            tempFile = File.createTempFile(uploadPath, null);
            //处理文件来源
            processFile(inputSource, tempFile);
            //上传文件到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            //获取图像信息对象，封装返回结果
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //获取到图像处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)){
                //获取压缩后的图片信息
                CIObject compressedObject = objectList.get(0);
                //缩略图默认就等于压缩图
                CIObject thumbnailCiObject = compressedObject;
                //有生成缩略图 才获取缩略图
                if (objectList.size() > 1){
                    thumbnailCiObject = objectList.get(1);
                }
                //封装压缩图片的返回结果
                return buildResult(originalFilename, compressedObject, thumbnailCiObject);
            }
            return buildResult(originalFilename, uploadPath, tempFile, imageInfo);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败" + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    /**
     * 封装返回结果
     * @param originalFilename
     * @param compressedObject
     * @param thumbnailCiObject
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedObject, CIObject thumbnailCiObject) {
        int picWidth = compressedObject.getWidth();
        int picHeight = compressedObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,  2).doubleValue();
        //封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        //设置压缩后的原图地址
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedObject.getKey());
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedObject.getFormat());
        //设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     * @param originalFilename
     * @param uploadPath
     * @param tempFile
     * @param imageInfo
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, String uploadPath, File tempFile, ImageInfo imageInfo) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight,  2).doubleValue();

        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(FileUtil.size(tempFile));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        return uploadPictureResult;
    }

    /**
     * 校验输入源 文件或url
     *
     * @param inputSource
     * @param tempFile
     */
    protected abstract void processFile(Object inputSource, File tempFile) throws IOException;

    /**
     * 获取输入源原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 删除临时文件
     * @param file
     */
    public  void deleteTempFile(File file){
        if (file != null) {
            boolean delete = file.delete();
            if (!delete) {
                System.out.println("删除临时文件失败！文件路径：" + file.getAbsolutePath());
            }
        }
    }



}
