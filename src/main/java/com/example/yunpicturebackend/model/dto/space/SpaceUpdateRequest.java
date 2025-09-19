package com.example.yunpicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 更新空间请求
 */
@Data
public class SpaceUpdateRequest implements Serializable {

    /**
     * 空间id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间等级
     */
    private Integer spaceLevel;

    /**
     * 空间最大容量
     */
    private Long maxSize;

    /**
     * 空间最大文件数量
     */
    private Long maxCount;

    private static final long serialVersionUID = 292981372044995988L;
}
