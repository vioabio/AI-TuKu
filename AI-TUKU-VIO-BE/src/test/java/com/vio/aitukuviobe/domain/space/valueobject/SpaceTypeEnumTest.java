package com.vio.aitukuviobe.domain.space.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaceTypeEnum 单元测试")
class SpaceTypeEnumTest {

    @Nested
    @DisplayName("getEnumByValue")
    class GetEnumByValue {

        @Test
        @DisplayName("0 应返回 PRIVATE")
        void shouldReturnPrivate() {
            assertThat(SpaceTypeEnum.getEnumByValue(0)).isEqualTo(SpaceTypeEnum.PRIVATE);
        }

        @Test
        @DisplayName("1 应返回 TEAM")
        void shouldReturnTeam() {
            assertThat(SpaceTypeEnum.getEnumByValue(1)).isEqualTo(SpaceTypeEnum.TEAM);
        }

        @Test
        @DisplayName("无效值应返回 null")
        void shouldReturnNullForInvalidValue() {
            assertThat(SpaceTypeEnum.getEnumByValue(99)).isNull();
        }
    }

    @Test
    @DisplayName("getValue / getText 应返回正确值")
    void shouldReturnCorrectValues() {
        assertThat(SpaceTypeEnum.PRIVATE.getValue()).isEqualTo(0);
        assertThat(SpaceTypeEnum.PRIVATE.getText()).isEqualTo("私有");
        assertThat(SpaceTypeEnum.TEAM.getValue()).isEqualTo(1);
        assertThat(SpaceTypeEnum.TEAM.getText()).isEqualTo("团队");
    }
}
