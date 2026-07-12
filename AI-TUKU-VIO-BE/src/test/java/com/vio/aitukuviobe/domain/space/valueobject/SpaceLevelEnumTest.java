package com.vio.aitukuviobe.domain.space.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpaceLevelEnum 单元测试")
class SpaceLevelEnumTest {

    @Nested
    @DisplayName("getEnumByValue")
    class GetEnumByValue {

        @Test
        @DisplayName("0 应返回 COMMON")
        void shouldReturnCommon() {
            assertThat(SpaceLevelEnum.getEnumByValue(0)).isEqualTo(SpaceLevelEnum.COMMON);
        }

        @Test
        @DisplayName("1 应返回 PROFESSIONAL")
        void shouldReturnProfessional() {
            assertThat(SpaceLevelEnum.getEnumByValue(1)).isEqualTo(SpaceLevelEnum.PROFESSIONAL);
        }

        @Test
        @DisplayName("2 应返回 FLAGSHIP")
        void shouldReturnFlagship() {
            assertThat(SpaceLevelEnum.getEnumByValue(2)).isEqualTo(SpaceLevelEnum.FLAGSHIP);
        }

        @Test
        @DisplayName("无效值应返回 null")
        void shouldReturnNullForInvalidValue() {
            assertThat(SpaceLevelEnum.getEnumByValue(99)).isNull();
        }
    }

    @Nested
    @DisplayName("各等级配额")
    class QuotaValues {

        @Test
        @DisplayName("COMMON: maxSize=100MB, maxCount=100")
        void commonLevelQuota() {
            assertThat(SpaceLevelEnum.COMMON.getMaxSize()).isEqualTo(100L * 1024 * 1024);
            assertThat(SpaceLevelEnum.COMMON.getMaxCount()).isEqualTo(100L);
            assertThat(SpaceLevelEnum.COMMON.getText()).isEqualTo("普通版");
        }

        @Test
        @DisplayName("PROFESSIONAL: maxSize=1000MB, maxCount=1000")
        void professionalLevelQuota() {
            assertThat(SpaceLevelEnum.PROFESSIONAL.getMaxSize()).isEqualTo(1000L * 1024 * 1024);
            assertThat(SpaceLevelEnum.PROFESSIONAL.getMaxCount()).isEqualTo(1000L);
            assertThat(SpaceLevelEnum.PROFESSIONAL.getText()).isEqualTo("专业版");
        }

        @Test
        @DisplayName("FLAGSHIP: maxSize=10000MB, maxCount=10000")
        void flagshipLevelQuota() {
            assertThat(SpaceLevelEnum.FLAGSHIP.getMaxSize()).isEqualTo(10000L * 1024 * 1024);
            assertThat(SpaceLevelEnum.FLAGSHIP.getMaxCount()).isEqualTo(10000L);
            assertThat(SpaceLevelEnum.FLAGSHIP.getText()).isEqualTo("旗舰版");
        }
    }

    @Test
    @DisplayName("getValue 应返回正确的整数值")
    void shouldReturnCorrectValue() {
        assertThat(SpaceLevelEnum.COMMON.getValue()).isEqualTo(0);
        assertThat(SpaceLevelEnum.PROFESSIONAL.getValue()).isEqualTo(1);
        assertThat(SpaceLevelEnum.FLAGSHIP.getValue()).isEqualTo(2);
    }
}
