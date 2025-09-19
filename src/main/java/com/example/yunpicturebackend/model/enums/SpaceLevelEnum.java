package com.example.yunpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间等级枚举
 */
@Getter
public enum SpaceLevelEnum {

    COMMON("普通空间", 0, 1024 * 1024 * 100L, 100),
    PROFESSIONAL("高级空间", 1, 1024 * 1024 * 1000L, 1000),
    FLAGSHIP("旗舰空间", 2, 1024 * 1024 * 10000L, 10000),
    ;

    private final String text;
    private final int value;
    private final long maxSize;
    private final long maxCount;

    SpaceLevelEnum(String text, int value, long maxSize, long maxCount) {
        this.text = text;
        this.value = value;
        this.maxSize = maxSize;
        this.maxCount = maxCount;
    }

    /**
     * 根据value获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static SpaceLevelEnum getEnumByValue(int value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceLevelEnum spaceLevelEnum : SpaceLevelEnum.values()) {
            if (spaceLevelEnum.value == value) {
                return spaceLevelEnum;
            }
        }
        return null;
    }
}
