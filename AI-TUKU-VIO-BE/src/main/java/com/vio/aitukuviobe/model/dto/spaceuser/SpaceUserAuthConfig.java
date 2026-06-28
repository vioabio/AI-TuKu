package com.vio.aitukuviobe.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间成员权限配置
 */
@Data
public class SpaceUserAuthConfig implements Serializable {
    private List<Permission> permissions;
    private List<Role> roles;

    @Data
    public static class Permission {
        private String key;
        private String name;
        private String description;
    }

    @Data
    public static class Role {
        private String key;
        private String name;
        private List<String> permissions;
        private String description;
    }

    private static final long serialVersionUID = 1L;
}
