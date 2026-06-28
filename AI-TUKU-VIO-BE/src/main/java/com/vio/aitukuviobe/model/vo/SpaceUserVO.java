package com.vio.aitukuviobe.model.vo;

import com.vio.aitukuviobe.model.entity.SpaceUser;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间成员视图对象
 */
@Data
public class SpaceUserVO implements Serializable {
    private Long id;
    private Long spaceId;
    private Long userId;
    private String spaceRole;
    private Date createTime;
    private Date updateTime;
    /** 用户信息 */
    private UserVO user;
    /** 空间信息 */
    private SpaceVO space;
    private static final long serialVersionUID = 1L;

    public static SpaceUserVO objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) return null;
        SpaceUserVO spaceUserVO = new SpaceUserVO();
        BeanUtils.copyProperties(spaceUser, spaceUserVO);
        return spaceUserVO;
    }

    public static SpaceUser voToObj(SpaceUserVO spaceUserVO) {
        if (spaceUserVO == null) return null;
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }
}
