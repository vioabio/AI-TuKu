package com.vio.aitukuviobe.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vio.aitukuviobe.model.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.model.dto.spaceuser.SpaceUserQueryRequest;
import com.vio.aitukuviobe.model.entity.SpaceUser;
import com.vio.aitukuviobe.model.vo.SpaceUserVO;

import java.util.List;

public interface SpaceUserService extends IService<SpaceUser> {

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    void validSpaceUser(SpaceUser spaceUser, boolean add);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
