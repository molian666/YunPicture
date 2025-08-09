package com.example.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id
     */
    private Long id;

    /**
     * 图片url
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    private static final long serialVersionUID = 4419821334926124927L;

}
