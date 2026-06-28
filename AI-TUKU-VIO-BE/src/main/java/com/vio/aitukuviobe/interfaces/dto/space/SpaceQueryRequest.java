package com.vio.aitukuviobe.interfaces.dto.space;

import com.vio.aitukuviobe.infrastructure.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 空间查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {
    private Long id;
    private Long userId;
    private String spaceName;
    private Integer spaceLevel;
    private Integer spaceType;
    private static final long serialVersionUID = 1L;
}