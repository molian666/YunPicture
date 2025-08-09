package com.example.yunpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * url图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File tempFile) throws IOException {
        String url = (String) inputSource;
        // 下载文件
        HttpUtil.downloadFile(url, tempFile);
        
        // 确保文件有正确的扩展名
        String fileName = getOriginalFilename(inputSource);
        String fileExtension = FileUtil.extName(fileName);
        if (StringUtils.isNotBlank(fileExtension)) {
            // 如果临时文件没有扩展名，重新命名
            String tempFileName = tempFile.getName();
            if (!tempFileName.contains(".")) {
                File newTempFile = new File(tempFile.getParentFile(), tempFileName + "." + fileExtension);
                if (tempFile.renameTo(newTempFile)) {
                    // 更新tempFile引用
                }
            }
        }
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String url = (String) inputSource;
        String fileName = FileUtil.getName(url);
        // 如果文件名为空或者不包含点号，使用默认名称
        if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
            fileName = "image_" + System.currentTimeMillis();
        }
        return fileName;
    }

    @Override
    protected void validPicture(Object inputSource) {
        String url = (String) inputSource;
        //校验非空
        ThrowUtils.throwIf(StringUtils.isBlank(url), ErrorCode.PARAMS_ERROR, "图片url不能为空");
        
        //预处理URL，去除首尾空格
        url = url.trim();
        
        //校验url格式
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片url格式错误");
        }
        //校验url协议
        ThrowUtils.throwIf(!url.startsWith("http://") && !url.startsWith("https://"), ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议");
        //发送head请求验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, url).execute();
            //未正常返回无需其他判断
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //文件存在 文件类型校验
            String contentType = httpResponse.header("Content-Type");
            //不为空，才校验是否合法，这样校验规则较为宽松
            if (StringUtils.isNotBlank(contentType)){
                //允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "image/jpg");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()), ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            //文件大小校验
            String contentLengthStr= httpResponse.header("Content-Length");
            if (StringUtils.isNotBlank(contentLengthStr)){
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024;
                    ThrowUtils.throwIf(contentLength > 100 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小超出100MB");
                }catch (NumberFormatException e){
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超出100MB");
                }

            }
        } finally {
            if (httpResponse != null){
                httpResponse.close();
            }
        }
    }
}
