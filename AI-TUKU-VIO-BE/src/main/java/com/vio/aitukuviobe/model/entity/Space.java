package com.vio.aitukuviobe.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间
 * @TableName space
 */
@TableName(value = "space")
@Data
public class Space implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String spaceName;
    private Integer spaceLevel;
    private Long maxSize;
    private Long maxCount;
    private Long totalSize;
    private Long totalCount;
    private Long userId;
    private Date createTime;
    private Date editTime;
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
