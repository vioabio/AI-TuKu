package com.vio.aitukuviobe.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加空间成员请求
 */
@Data
public class SpaceUserAddRequest implements Serializable {
    private Long spaceId;
    private Long userId;
    /** 空间角色：viewer/editor/admin */
    private String spaceRole;
    private static final long serialVersionUID = 1L;
}
