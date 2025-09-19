package com.example.yunpicturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.yunpicturebackend.model.entity.Picture;
import com.example.yunpicturebackend.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 空间视图
 *
 * @TableName picture
 */
@TableName(value = "picture")
@Data
public class SpaceVO implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间等级
     */
    private Integer spaceLevel;

    /**
     * 图片名称
     */
    private String spaceName;

    /**
     * 最大容量
     */
    private Integer maxSize;

    /**
     * 最大数量
     */
    private Integer maxCount;

    /**
     * 总容量
     */
    private Integer totalSize;

    /**
     * 总数量
     */
    private Integer totalCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    /**
     * 创建用户信息
     */
    private UserVO user;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }

    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }
}