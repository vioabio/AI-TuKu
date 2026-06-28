package com.vio.aitukuviobe.interfaces.assembler;

import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.interfaces.dto.user.UserAddRequest;
import com.vio.aitukuviobe.interfaces.dto.user.UserUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 用户装配器（DTO ↔ Entity 转换）
 */
public class UserAssembler {

    public static User toUserEntity(UserAddRequest request) {
        if (request == null) return null;
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }

    public static User toUserEntity(UserUpdateRequest request) {
        if (request == null) return null;
        User user = new User();
        BeanUtils.copyProperties(request, user);
        return user;
    }
}
