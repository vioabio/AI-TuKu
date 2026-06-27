package com.vio.aitukuviobe.model.dto.space;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间编辑请求（用户编辑，仅允许编辑空间名称）
 */
@Data
public class SpaceEditRequest implements Serializable {
    private Long id;
    private String spaceName;
    private static final long serialVersionUID = 1L;
}