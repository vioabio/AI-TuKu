package com.vio.aitukuviobe.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.SpaceUserVO;

import java.util.List;

/**
 * 空间用户应用服务接口
 */
public interface SpaceUserApplicationService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
