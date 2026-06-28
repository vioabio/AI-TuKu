package com.vio.aitukuviobe.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询空间成员请求
 */
@Data
public class SpaceUserQueryRequest implements Serializable {
    private Long id;
    private Long spaceId;
    private Long userId;
    private String spaceRole;
    private static final long serialVersionUID = 1L;
}
