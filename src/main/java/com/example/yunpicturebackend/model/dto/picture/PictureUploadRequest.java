package com.example.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id
     */
    private Long id;
    private static final long serialVersionUID = 4419821334926124927L;

}
