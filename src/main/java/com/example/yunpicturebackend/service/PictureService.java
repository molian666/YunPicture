package com.example.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.yunpicturebackend.model.dto.picture.PictureQueryRequest;
import com.example.yunpicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.example.yunpicturebackend.model.dto.picture.PictureUploadRequest;
import com.example.yunpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.yunpicturebackend.model.entity.User;
import com.example.yunpicturebackend.model.vo.PictureVO;
import com.example.yunpicturebackend.model.vo.UserVO;
import com.qcloud.cos.model.MultipartUpload;
import org.springframework.web.multipart.MultipartFile;
import com.example.yunpicturebackend.model.dto.picture.PictureReviewRequest;

import javax.servlet.http.HttpServletRequest;

/**
* @author wyh
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2025-08-05 09:27:16
*/
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 获取查询条件
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片封装类
     * @param picture
     * @param httpServletRequest
     * @return
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest httpServletRequest);

    /**
     * 获取图片分页封装类
     * @param page
     * @param request
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> page, HttpServletRequest request);

    /**
     * 校验图片
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 图片审核
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 填充审核参数
     * @param picture
     * @param loginUser
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取上传图片
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer UploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

    /**
     * 清理图片文件
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);
}
