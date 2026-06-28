package com.vio.aitukuviobe.interfaces.assembler;

import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserAddRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间用户装配器（DTO ↔ Entity 转换）
 */
public class SpaceUserAssembler {

    public static SpaceUser toSpaceUserEntity(SpaceUserAddRequest request) {
        if (request == null) return null;
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }
}
