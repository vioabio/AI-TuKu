package com.vio.aitukuviobe.domain.space.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间成员
 * @TableName space_user
 */
@TableName(value = "space_user")
@Data
public class SpaceUser implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long spaceId;
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private Date createTime;
    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
