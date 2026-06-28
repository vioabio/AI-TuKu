package com.vio.aitukuviobe.interfaces.assembler;

import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceAddRequest;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceEditRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间装配器（DTO ↔ Entity 转换）
 */
public class SpaceAssembler {

    public static Space toSpaceEntity(SpaceAddRequest request) {
        if (request == null) return null;
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }

    public static Space toSpaceEntity(SpaceEditRequest request) {
        if (request == null) return null;
        Space space = new Space();
        BeanUtils.copyProperties(request, space);
        return space;
    }
}
