package com.vio.aitukuviobe.interfaces.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间更新请求（管理员更新，可修改级别和限额）
 */
@Data
public class SpaceUpdateRequest implements Serializable {
    private Long id;
    private String spaceName;
    private Integer spaceLevel;
    private Long maxSize;
    private Long maxCount;
    private static final long serialVersionUID = 1L;
}