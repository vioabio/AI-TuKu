package com.vio.aitukuviobe.domain.picture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.picture.entity.Picture;
import com.vio.aitukuviobe.domain.picture.repository.PictureRepository;
import com.vio.aitukuviobe.domain.picture.valueobject.PictureReviewStatusEnum;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.api.CosManager;
import com.vio.aitukuviobe.infrastructure.api.aliyunai.AliYunAiApi;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.infrastructure.manager.upload.FilePictureUpload;
import com.vio.aitukuviobe.infrastructure.manager.upload.UrlPictureUpload;
import com.vio.aitukuviobe.interfaces.dto.picture.PictureEditRequest;
import com.vio.aitukuviobe.interfaces.dto.picture.PictureQueryRequest;
import com.vio.aitukuviobe.interfaces.dto.picture.PictureReviewRequest;
import com.vio.aitukuviobe.testbase.BaseUnitTest;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("PictureDomainServiceImpl 单元测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class PictureDomainServiceImplTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserDomainService userDomainService;
    @Mock
    private PictureRepository pictureRepository;
    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private CosManager cosManager;
    @Mock
    private FilePictureUpload filePictureUpload;
    @Mock
    private UrlPictureUpload urlPictureUpload;
    @Mock
    private AliYunAiApi aliYunAiApi;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PictureDomainServiceImpl pictureDomainService;

    private User normalUser;
    private User adminUser;
    private Picture testPicture;

    @BeforeEach
    void setUp() {
        normalUser = TestDataFactory.aUser();
        normalUser.setId(1L);

        adminUser = TestDataFactory.anAdminUser();
        adminUser.setId(2L);

        testPicture = TestDataFactory.aPicture(null, 1L);
        testPicture.setId(1L);

        // Mock TransactionTemplate 回调执行
        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    // ==================== ValidPicture ====================

    @Nested
    @DisplayName("图片校验")
    class ValidPicture {

        @Test
        @DisplayName("有效图片 → 应通过校验")
        void shouldPassForValidPicture() {
            testPicture.setUrl("https://example.com/test.jpg");
            testPicture.setIntroduction("简介");
            pictureDomainService.validPicture(testPicture);
        }

        @Test
        @DisplayName("picture 为 null → 应抛异常")
        void shouldThrowWhenPictureIsNull() {
            assertThatThrownBy(() -> pictureDomainService.validPicture(null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("id 为 null → 应抛异常")
        void shouldThrowWhenIdIsNull() {
            testPicture.setId(null);
            assertThatThrownBy(() -> pictureDomainService.validPicture(testPicture))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("id 不能为空");
        }

        @Test
        @DisplayName("url > 1024 字符 → 应抛异常")
        void shouldThrowWhenUrlTooLong() {
            testPicture.setUrl("https://" + "a".repeat(2000) + ".com/img.jpg");
            assertThatThrownBy(() -> pictureDomainService.validPicture(testPicture))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("url 过长");
        }

        @Test
        @DisplayName("简介 > 800 字符 → 应抛异常")
        void shouldThrowWhenIntroductionTooLong() {
            testPicture.setIntroduction("x".repeat(900));
            assertThatThrownBy(() -> pictureDomainService.validPicture(testPicture))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("简介过长");
        }
    }

    // ==================== FillReviewParams ====================

    @Nested
    @DisplayName("审核参数填充")
    class FillReviewParams {

        @Test
        @DisplayName("管理员上传 → 自动过审")
        void shouldAutoPassForAdminUpload() {
            given(userDomainService.isAdmin(adminUser)).willReturn(true);

            pictureDomainService.fillReviewParams(testPicture, adminUser);

            assertThat(testPicture.getReviewStatus()).isEqualTo(PictureReviewStatusEnum.PASS.getValue());
            assertThat(testPicture.getReviewerId()).isEqualTo(adminUser.getId());
            assertThat(testPicture.getReviewMessage()).isEqualTo("管理员自动过审");
            assertThat(testPicture.getReviewTime()).isNotNull();
        }

        @Test
        @DisplayName("普通用户上传 → 标记为待审核")
        void shouldSetReviewingForNormalUser() {
            given(userDomainService.isAdmin(normalUser)).willReturn(false);

            pictureDomainService.fillReviewParams(testPicture, normalUser);

            assertThat(testPicture.getReviewStatus()).isEqualTo(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    // ==================== EditPicture ====================

    @Nested
    @DisplayName("编辑图片")
    class EditPicture {

        @Test
        @DisplayName("正常编辑 → 应成功更新")
        void shouldEditSuccessfully() {
            testPicture.setUserId(1L);
            given(pictureRepository.getById(1L)).willReturn(testPicture);
            given(userDomainService.isAdmin(normalUser)).willReturn(false);
            given(pictureRepository.updateById(any(Picture.class))).willReturn(true);

            PictureEditRequest req = new PictureEditRequest();
            req.setId(1L);
            req.setName("新名称");
            req.setIntroduction("新简介");
            req.setTags(Arrays.asList("新标签"));

            pictureDomainService.editPicture(req, normalUser);

            verify(pictureRepository).updateById(any(Picture.class));
        }

        @Test
        @DisplayName("图片不存在 → 应抛 NOT_FOUND_ERROR")
        void shouldThrowWhenPictureNotFound() {
            given(pictureRepository.getById(99L)).willReturn(null);

            PictureEditRequest req = new PictureEditRequest();
            req.setId(99L);

            assertThatThrownBy(() -> pictureDomainService.editPicture(req, normalUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
        }

        @Test
        @DisplayName("非所有者编辑公开图 → 应抛 NO_AUTH_ERROR")
        void shouldThrowWhenNotOwnerEditsPublic() {
            testPicture.setUserId(2L); // 图片属于 userId=2
            testPicture.setSpaceId(null); // 公开图
            given(pictureRepository.getById(1L)).willReturn(testPicture);
            given(userDomainService.isAdmin(normalUser)).willReturn(false);

            PictureEditRequest req = new PictureEditRequest();
            req.setId(1L);

            assertThatThrownBy(() -> pictureDomainService.editPicture(req, normalUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
        }
    }

    // ==================== DeletePicture ====================

    @Nested
    @DisplayName("删除图片")
    class DeletePicture {

        @Test
        @DisplayName("图片不存在 → 应抛 NOT_FOUND_ERROR")
        void shouldThrowWhenPictureNotFound() {
            given(pictureRepository.getById(99L)).willReturn(null);

            assertThatThrownBy(() -> pictureDomainService.deletePicture(99L, normalUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
        }

        @Test
        @DisplayName("pictureId ≤ 0 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenIdInvalid() {
            assertThatThrownBy(() -> pictureDomainService.deletePicture(0L, normalUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("user 为 null → 应抛 NO_AUTH_ERROR")
        void shouldThrowWhenUserIsNull() {
            assertThatThrownBy(() -> pictureDomainService.deletePicture(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
        }
    }

    // ==================== CheckPictureAuth ====================

    @Nested
    @DisplayName("图片权限校验")
    class CheckPictureAuth {

        @Test
        @DisplayName("公开图 → 所有者有权限")
        void shouldPassForPublicPictureOwner() {
            testPicture.setUserId(1L);
            testPicture.setSpaceId(null);

            pictureDomainService.checkPictureAuth(normalUser, testPicture);
        }

        @Test
        @DisplayName("公开图 → 管理员有权限")
        void shouldPassForPublicPictureAdmin() {
            testPicture.setUserId(2L);
            testPicture.setSpaceId(null);
            given(userDomainService.isAdmin(adminUser)).willReturn(true);

            pictureDomainService.checkPictureAuth(adminUser, testPicture);
        }

        @Test
        @DisplayName("公开图 → 陌生人无权限")
        void shouldThrowForPublicPictureStranger() {
            testPicture.setUserId(2L);
            testPicture.setSpaceId(null);
            given(userDomainService.isAdmin(normalUser)).willReturn(false);

            assertThatThrownBy(() -> pictureDomainService.checkPictureAuth(normalUser, testPicture))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
        }

        @Test
        @DisplayName("空间图片 → 所有者有权限")
        void shouldPassForSpacePictureOwner() {
            testPicture.setUserId(1L);
            testPicture.setSpaceId(100L);

            pictureDomainService.checkPictureAuth(normalUser, testPicture);
        }

        @Test
        @DisplayName("空间图片 → 非所有者无权限")
        void shouldThrowForSpacePictureNonOwner() {
            testPicture.setUserId(2L);
            testPicture.setSpaceId(100L);

            assertThatThrownBy(() -> pictureDomainService.checkPictureAuth(normalUser, testPicture))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
        }
    }

    // ==================== DoPictureReview ====================

    @Nested
    @DisplayName("图片审核")
    class DoPictureReview {

        @Test
        @DisplayName("正常审核 → 应更新审核状态")
        void shouldReviewSuccessfully() {
            testPicture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
            given(pictureRepository.getById(1L)).willReturn(testPicture);
            given(pictureRepository.updateById(any(Picture.class))).willReturn(true);

            PictureReviewRequest req = new PictureReviewRequest();
            req.setId(1L);
            req.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

            pictureDomainService.doPictureReview(req, adminUser);

            verify(pictureRepository).updateById(any(Picture.class));
        }

        @Test
        @DisplayName("重复审核 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenDuplicateReview() {
            testPicture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            given(pictureRepository.getById(1L)).willReturn(testPicture);

            PictureReviewRequest req = new PictureReviewRequest();
            req.setId(1L);
            req.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

            assertThatThrownBy(() -> pictureDomainService.doPictureReview(req, adminUser))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("请勿重复审核");
        }

        @Test
        @DisplayName("审核状态为 REVIEWING → 应抛 PARAMS_ERROR")
        void shouldThrowWhenReviewStatusIsReviewing() {
            PictureReviewRequest req = new PictureReviewRequest();
            req.setId(1L);
            req.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());

            assertThatThrownBy(() -> pictureDomainService.doPictureReview(req, adminUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("图片不存在 → 应抛 NOT_FOUND_ERROR")
        void shouldThrowWhenPictureNotFoundInReview() {
            given(pictureRepository.getById(99L)).willReturn(null);

            PictureReviewRequest req = new PictureReviewRequest();
            req.setId(99L);
            req.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

            assertThatThrownBy(() -> pictureDomainService.doPictureReview(req, adminUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
        }
    }

    // ==================== GetQueryWrapper ====================

    @Nested
    @DisplayName("图片查询条件构建")
    class GetQueryWrapper {

        @Test
        @DisplayName("request 为 null → 应返回空 wrapper")
        void shouldReturnEmptyWrapperForNullRequest() {
            QueryWrapper<Picture> wrapper = pictureDomainService.getQueryWrapper(null);
            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("searchText → 应构建 name/introduction OR 条件")
        void shouldBuildOrConditionForSearchText() {
            PictureQueryRequest req = new PictureQueryRequest();
            req.setSearchText("风景");

            QueryWrapper<Picture> wrapper = pictureDomainService.getQueryWrapper(req);

            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("标签过滤 → 应构建 LIKE 条件")
        void shouldBuildLikeConditionForTags() {
            PictureQueryRequest req = new PictureQueryRequest();
            req.setTags(Arrays.asList("风景", "自然"));

            QueryWrapper<Picture> wrapper = pictureDomainService.getQueryWrapper(req);

            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("nullSpaceId → 应添加 IS NULL 条件")
        void shouldBuildNullSpaceIdCondition() {
            PictureQueryRequest req = new PictureQueryRequest();
            req.setNullSpaceId(true);

            QueryWrapper<Picture> wrapper = pictureDomainService.getQueryWrapper(req);

            assertThat(wrapper).isNotNull();
        }
    }
}
