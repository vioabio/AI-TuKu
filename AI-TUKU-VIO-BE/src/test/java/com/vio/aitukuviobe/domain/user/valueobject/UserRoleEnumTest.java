package com.vio.aitukuviobe.domain.user.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRoleEnum 单元测试")
class UserRoleEnumTest {

    @Nested
    @DisplayName("getEnumByValue")
    class GetEnumByValue {

        @Test
        @DisplayName("应返回 USER 枚举")
        void shouldReturnUser() {
            assertThat(UserRoleEnum.getEnumByValue("user")).isEqualTo(UserRoleEnum.USER);
        }

        @Test
        @DisplayName("应返回 VIP 枚举")
        void shouldReturnVip() {
            assertThat(UserRoleEnum.getEnumByValue("vip")).isEqualTo(UserRoleEnum.VIP);
        }

        @Test
        @DisplayName("应返回 ADMIN 枚举")
        void shouldReturnAdmin() {
            assertThat(UserRoleEnum.getEnumByValue("admin")).isEqualTo(UserRoleEnum.ADMIN);
        }

        @Test
        @DisplayName("无效值应返回 null")
        void shouldReturnNullForInvalidValue() {
            assertThat(UserRoleEnum.getEnumByValue("superadmin")).isNull();
        }

        @Test
        @DisplayName("null 应返回 null")
        void shouldReturnNullForNullValue() {
            assertThat(UserRoleEnum.getEnumByValue(null)).isNull();
        }
    }

    @Test
    @DisplayName("getValue 应返回正确的字符串")
    void shouldReturnCorrectValue() {
        assertThat(UserRoleEnum.USER.getValue()).isEqualTo("user");
        assertThat(UserRoleEnum.ADMIN.getValue()).isEqualTo("admin");
        assertThat(UserRoleEnum.VIP.getValue()).isEqualTo("vip");
    }
}
