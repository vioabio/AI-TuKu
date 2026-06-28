package com.vio.aitukuviobe.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间成员请求
 */
@Data
public class SpaceUserEditRequest implements Serializable {
    private Long id;
    /** 空间角色：viewer/editor/admin */
    private String spaceRole;
    private static final long serialVersionUID = 1L;
}
