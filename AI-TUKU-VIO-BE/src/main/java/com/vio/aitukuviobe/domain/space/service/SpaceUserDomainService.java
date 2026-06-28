package com.vio.aitukuviobe.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
 * 空间用户领域服务接口
 */
public interface SpaceUserDomainService {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
