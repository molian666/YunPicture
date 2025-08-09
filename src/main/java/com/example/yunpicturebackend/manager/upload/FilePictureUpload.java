package com.example.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File tempFile) throws IOException {
        MultipartFile file = (MultipartFile) inputSource;
        file.transferTo(tempFile);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile file = (MultipartFile) inputSource;
        String originalFilename = file.getOriginalFilename();
        return originalFilename;
    }

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile file = (MultipartFile) inputSource;
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        //校验文件大小
        long fileSize = file.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 100 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小超出100MB");
        //校验文件后缀
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        //允许上传的文件后缀和格式列表
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件格式错误");
    }
}
