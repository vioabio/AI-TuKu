package com.vio.aitukuviobe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.model.dto.space.SpaceAddRequest;
import com.vio.aitukuviobe.model.dto.space.SpaceQueryRequest;
import com.vio.aitukuviobe.model.entity.Space;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @description 针对表【space(空间)】的数据库操作Service
 */
public interface SpaceService extends IService<Space> {

    /**
     * 校验空间
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别自动填充限额
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 获取空间封装类
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取查询条件
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 校验空间权限（仅空间创建人可操作）
     */
    void checkSpaceAuth(User loginUser, Space space);
}