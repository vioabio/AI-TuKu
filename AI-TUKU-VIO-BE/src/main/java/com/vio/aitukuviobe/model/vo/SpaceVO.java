package com.vio.aitukuviobe.model.vo;

import com.vio.aitukuviobe.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 空间视图对象
 */
@Data
public class SpaceVO implements Serializable {
    private Long id;
    private String spaceName;
    private Integer spaceLevel;
    private Long maxSize;
    private Long maxCount;
    private Long totalSize;
    private Long totalCount;
    private Long userId;
    private Integer spaceType;
    private Date createTime;
    private Date editTime;
    private Date updateTime;
    private UserVO user;
    /** 当前用户对该空间的权限列表 */
    private List<String> permissionList = new ArrayList<>();
    private static final long serialVersionUID = 1L;

    /**
     * 对象转封装类
     */
    public static SpaceVO objToVo(Space space) {
        if (space == null) return null;
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }

    /**
     * 封装类转对象
     */
    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) return null;
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }
}
