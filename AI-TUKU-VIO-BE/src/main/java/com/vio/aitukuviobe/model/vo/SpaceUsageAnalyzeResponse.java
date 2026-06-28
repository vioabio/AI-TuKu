package com.vio.aitukuviobe.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间资源使用分析响应
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {

    /** 已使用大小 */
    private Long usedSize;
    /** 最大大小限制 */
    private Long maxSize;
    /** 空间使用比例 0-100 */
    private Double sizeUsageRatio;
    /** 当前图片数量 */
    private Long usedCount;
    /** 最大图片数量限制 */
    private Long maxCount;
    /** 图片数量使用比例 0-100 */
    private Double countUsageRatio;

    private static final long serialVersionUID = 1L;
}
