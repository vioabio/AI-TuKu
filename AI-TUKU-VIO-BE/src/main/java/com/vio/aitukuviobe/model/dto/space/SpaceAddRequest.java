package com.vio.aitukuviobe.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间添加请求
 */
@Data
public class SpaceAddRequest implements Serializable {
    private String spaceName;
    private Integer spaceLevel;
    private static final long serialVersionUID = 1L;
}
