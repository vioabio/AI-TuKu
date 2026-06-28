package com.vio.aitukuviobe.interfaces.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用空间分析请求基类
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {

    /**
     * 空间 ID（指定空间分析时使用）
     */
    private Long spaceId;

    /**
     * 是否查询所有空间（管理员权限）
     */
    private boolean queryAll;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    private static final long serialVersionUID = 1L;
}
