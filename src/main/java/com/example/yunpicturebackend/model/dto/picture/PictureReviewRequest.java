package com.example.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 图片审核状态请求
 */
@Data
public class PictureReviewRequest implements Serializable {

    /**
     * 图片id
     */
    private Long reviewerId;

    /**
     * 审核状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核时间
     */
    private Date reviewTime;

    private static final long serialVersionUID = 4419821334926124927L;

}
