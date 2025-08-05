package com.example.yunpicturebackend.controller;

import com.example.yunpicturebackend.annotation.AuthCheck;
import com.example.yunpicturebackend.common.BaseResponse;
import com.example.yunpicturebackend.common.ResultUtils;
import com.example.yunpicturebackend.constant.UserConstant;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import com.example.yunpicturebackend.manager.CosManager;
import com.example.yunpicturebackend.model.enums.UserRoleEnum;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    /**
     * 测试文件上传
     * @param file
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile file){
        //文件目录
        String fileName = file.getOriginalFilename();
        String filePath = String.format("/test/%s", fileName);
        //上传文件
        File tempFile = null;
        try {
            tempFile = File.createTempFile(filePath, null);
            file.transferTo(tempFile);
            cosManager.putObject(filePath, tempFile);
            return ResultUtils.success(filePath);
        } catch (Exception e) {
            log.error("上传文件失败! 文件路径：" + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (tempFile != null) {
                boolean delete = tempFile.delete();
                if (!delete) {
                    log.error("删除临时文件失败！文件路径：" + tempFile.getAbsolutePath());
                }
            }
        }
    }

    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download")
    public void testDownload(String filePath, HttpServletResponse response) throws IOException {
        COSObject cosObject = cosManager.getObject(filePath, null);
        COSObjectInputStream objectContent = cosObject.getObjectContent();
        //设置响应头
        response.setContentType("application/octet-stream;charset=utf-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + filePath);
        //写入响应体
        response.getOutputStream().write(objectContent.readAllBytes());
        response.getOutputStream().flush();
        objectContent.close();
    }
}
