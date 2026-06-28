package com.vio.aitukuviobe.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceAddRequest;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 空间应用服务接口
 */
public interface SpaceApplicationService extends IService<Space> {

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    void checkSpaceAuth(User loginUser, Space space);
}
