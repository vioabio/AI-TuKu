package com.vio.aitukuviobe.domain.space.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaceRoleEnum 单元测试")
class SpaceRoleEnumTest {

    @Nested
    @DisplayName("getEnumByValue")
    class GetEnumByValue {

        @Test
        @DisplayName("应返回 VIEWER")
        void shouldReturnViewer() {
            assertThat(SpaceRoleEnum.getEnumByValue("viewer")).isEqualTo(SpaceRoleEnum.VIEWER);
        }

        @Test
        @DisplayName("应返回 EDITOR")
        void shouldReturnEditor() {
            assertThat(SpaceRoleEnum.getEnumByValue("editor")).isEqualTo(SpaceRoleEnum.EDITOR);
        }

        @Test
        @DisplayName("应返回 ADMIN")
        void shouldReturnAdmin() {
            assertThat(SpaceRoleEnum.getEnumByValue("admin")).isEqualTo(SpaceRoleEnum.ADMIN);
        }

        @Test
        @DisplayName("无效值应返回 null")
        void shouldReturnNullForInvalidValue() {
            assertThat(SpaceRoleEnum.getEnumByValue("owner")).isNull();
        }
    }

    @Test
    @DisplayName("getValue 应返回正确的字符串")
    void shouldReturnCorrectValue() {
        assertThat(SpaceRoleEnum.VIEWER.getValue()).isEqualTo("viewer");
        assertThat(SpaceRoleEnum.EDITOR.getValue()).isEqualTo("editor");
        assertThat(SpaceRoleEnum.ADMIN.getValue()).isEqualTo("admin");
    }
}
