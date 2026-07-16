package com.vio.aitukuviobe.domain.picture.entity;

import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Picture 实体单元测试")
class PictureTest {

    @Nested
    @DisplayName("审核状态流转")
    class ReviewStatusTransition {

        @Test
        @DisplayName("待审核 → 通过")
        void shouldTransitionFromPendingToPass() {
            Picture picture = TestDataFactory.aPicture(null, 1L).build();
            picture.setReviewStatus(PictureReviewStatusEnum.PENDING.getValue());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewMessage("审核通过");

            assertThat(picture.getReviewStatus()).isEqualTo(PictureReviewStatusEnum.PASS.getValue());
            assertThat(picture.getReviewMessage()).isEqualTo("审核通过");
        }

        @Test
        @DisplayName("待审核 → 拒绝")
        void shouldTransitionFromPendingToReject() {
            Picture picture = TestDataFactory.aPicture(null, 1L).build();
            picture.setReviewStatus(PictureReviewStatusEnum.PENDING.getValue());
            picture.setReviewStatus(PictureReviewStatusEnum.REJECT.getValue());
            picture.setReviewMessage("违规内容");

            assertThat(picture.getReviewStatus()).isEqualTo(PictureReviewStatusEnum.REJECT.getValue());
        }
    }

    @Nested
    @DisplayName("字段默认值")
    class FieldDefaults {

        @Test
        @DisplayName("新建图片应有默认审核状态")
        void newPictureShouldHaveDefaultReviewStatus() {
            Picture picture = TestDataFactory.aPicture(null, 1L).build();

            assertThat(picture.getReviewStatus()).isEqualTo(PictureReviewStatusEnum.PASS.getValue());
            assertThat(picture.getIsDelete()).isEqualTo(0);
        }

        @Test
        @DisplayName("空间图片应正确关联 spaceId")
        void spacePictureShouldHaveCorrectSpaceId() {
            Picture picture = TestDataFactory.aPicture(100L, 1L).build();

            assertThat(picture.getSpaceId()).isEqualTo(100L);
            assertThat(picture.getUserId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("图片属性")
    class PictureProperties {

        @Test
        @DisplayName("应正确计算宽高比")
        void shouldStoreCorrectAspectRatio() {
            Picture picture = TestDataFactory.aPicture(null, 1L)
                    .picWidth(1920).picHeight(1080).picScale(16.0 / 9.0).build();

            assertThat(picture.getPicWidth()).isEqualTo(1920);
            assertThat(picture.getPicHeight()).isEqualTo(1080);
            assertThat(picture.getPicScale()).isCloseTo(1.777, within(0.01));
        }
    }
}
