package com.vio.aitukuviobe.domain.space.valueobject;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 空间成员角色枚举
 */
@Getter
public enum SpaceRoleEnum {
    VIEWER("浏览者", "viewer"),
    EDITOR("编辑者", "editor"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static SpaceRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) return null;
        for (SpaceRoleEnum anEnum : SpaceRoleEnum.values()) {
            if (anEnum.value.equals(value)) return anEnum;
        }
        return null;
    }
}
