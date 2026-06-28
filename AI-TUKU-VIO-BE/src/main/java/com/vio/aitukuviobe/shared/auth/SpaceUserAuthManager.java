package com.vio.aitukuviobe.shared.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.vio.aitukuviobe.application.service.SpaceUserApplicationService;
import com.vio.aitukuviobe.application.service.UserApplicationService;
import com.vio.aitukuviobe.shared.auth.model.SpaceUserAuthConfig;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceRoleEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceTypeEnum;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    /**
     * 根据角色获取权限列表
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) return new ArrayList<>();
        SpaceUserAuthConfig.Role role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> spaceUserRole.equals(r.getKey()))
                .findFirst().orElse(null);
        if (role == null) return new ArrayList<>();
        return role.getPermissions();
    }

    /**
     * 获取用户在指定空间的权限列表
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) return new ArrayList<>();
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        if (space == null) {
            if (userApplicationService.isAdmin(loginUser)) return ADMIN_PERMISSIONS;
            return new ArrayList<>();
        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) return new ArrayList<>();
        switch (spaceTypeEnum) {
            case PRIVATE:
                if (space.getUserId().equals(loginUser.getId()) || userApplicationService.isAdmin(loginUser))
                    return ADMIN_PERMISSIONS;
                else return new ArrayList<>();
            case TEAM:
                SpaceUser spaceUser = spaceUserApplicationService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId()).one();
                if (spaceUser == null) return new ArrayList<>();
                return getPermissionsByRole(spaceUser.getSpaceRole());
        }
        return new ArrayList<>();
    }
}
