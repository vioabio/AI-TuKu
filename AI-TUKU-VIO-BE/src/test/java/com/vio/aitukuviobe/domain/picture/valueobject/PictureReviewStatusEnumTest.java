package com.vio.aitukuviobe.domain.picture.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PictureReviewStatusEnum 单元测试")
class PictureReviewStatusEnumTest {

    @Nested
    @DisplayName("getEnumByValue")
    class GetEnumByValue {

        @Test
        @DisplayName("0 应返回 REVIEWING")
        void shouldReturnReviewing() {
            assertThat(PictureReviewStatusEnum.getEnumByValue(0)).isEqualTo(PictureReviewStatusEnum.REVIEWING);
        }

        @Test
        @DisplayName("1 应返回 PASS")
        void shouldReturnPass() {
            assertThat(PictureReviewStatusEnum.getEnumByValue(1)).isEqualTo(PictureReviewStatusEnum.PASS);
        }

        @Test
        @DisplayName("2 应返回 REJECT")
        void shouldReturnReject() {
            assertThat(PictureReviewStatusEnum.getEnumByValue(2)).isEqualTo(PictureReviewStatusEnum.REJECT);
        }

        @Test
        @DisplayName("无效值应返回 null")
        void shouldReturnNullForInvalidValue() {
            assertThat(PictureReviewStatusEnum.getEnumByValue(99)).isNull();
        }

        @Test
        @DisplayName("null 应返回 null")
        void shouldReturnNullForNullValue() {
            assertThat(PictureReviewStatusEnum.getEnumByValue(null)).isNull();
        }
    }

    @Test
    @DisplayName("getValue 应返回正确的整数值")
    void shouldReturnCorrectValue() {
        assertThat(PictureReviewStatusEnum.REVIEWING.getValue()).isEqualTo(0);
        assertThat(PictureReviewStatusEnum.PASS.getValue()).isEqualTo(1);
        assertThat(PictureReviewStatusEnum.REJECT.getValue()).isEqualTo(2);
    }
}
