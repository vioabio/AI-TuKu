package com.vio.aitukuviobe.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceAddRequest;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceQueryRequest;

/**
 * 空间领域服务接口
 */
public interface SpaceDomainService {

    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void checkSpaceAuth(User loginUser, Space space);

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);
}
