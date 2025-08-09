package com.example.yunpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.example.yunpicturebackend.common.ResultUtils;
import com.example.yunpicturebackend.config.CosClientConfig;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.exception.ThrowUtils;
import com.example.yunpicturebackend.model.dto.file.UploadPictureResult;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.OriginalInfo;
import com.qcloud.cos.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @Deprecated 弃用, 改用upload包的模板方法
 */
@Component
@Slf4j
@Service
@Deprecated
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    @Resource
    private CosManager cosManager;

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        //校验图片
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
        String uplaodFileName = String.format("%s %s %s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uplaodFileName);
        //解析结果
        File tempFile = null;
        try {
            tempFile = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(tempFile);
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            //获取图像信息对象
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //封装返回结果
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
        } catch (Exception e) {
            log.error("上传文件失败! 文件路径：" + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(tempFile);
        }
    }

    /**
     * 删除临时文件
     * @param file
     */
    public  void deleteTempFile(File file){
        if (file != null) {
            boolean delete = file.delete();
            if (!delete) {
                log.error("删除临时文件失败！文件路径：" + file.getAbsolutePath());
            }
        }
    }

    private void validPicture(MultipartFile multipartFile){
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        //校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 100 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小超出100MB");
        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        //允许上传的文件后缀和格式列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式错误");
    }

}
