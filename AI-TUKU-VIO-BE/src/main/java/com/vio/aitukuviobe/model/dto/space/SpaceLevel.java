package com.vio.aitukuviobe.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间级别信息（用于前端展示）
 */
@Data
@AllArgsConstructor
public class SpaceLevel {
    private int value;
    private String text;
    private long maxCount;
    private long maxSize;
}
