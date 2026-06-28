package com.vio.aitukuviobe.domain.space.valueobject;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间类型枚举
 */
@Getter
public enum SpaceTypeEnum {
    PRIVATE(0, "私有"),
    TEAM(1, "团队");

    private final int value;
    private final String text;

    SpaceTypeEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) return null;
        for (SpaceTypeEnum anEnum : SpaceTypeEnum.values()) {
            if (anEnum.value == value) return anEnum;
        }
        return null;
    }
}
