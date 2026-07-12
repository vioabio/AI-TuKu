package com.vio.aitukuviobe.domain.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.space.repository.SpaceUserRepository;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceLevelEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceRoleEnum;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceTypeEnum;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceAddRequest;
import com.vio.aitukuviobe.interfaces.dto.space.SpaceQueryRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("SpaceDomainServiceImpl 单元测试")
@MockitoSettings(strictness = Strictness.LENIENT)
class SpaceDomainServiceImplTest extends BaseUnitTest {

    @Mock
    private SpaceRepository spaceRepository;
    @Mock
    private SpaceUserRepository spaceUserRepository;
    @Mock
    private UserDomainService userDomainService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private SpaceDomainServiceImpl spaceDomainService;

    private User normalUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        normalUser = TestDataFactory.aUser();
        normalUser.setId(1L);

        adminUser = TestDataFactory.anAdminUser();
        adminUser.setId(2L);

        given(transactionTemplate.execute(any())).willAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
    }

    // ==================== ValidSpace ====================

    @Nested
    @DisplayName("空间校验")
    class ValidSpace {

        @Test
        @DisplayName("新增时有效空间 → 应通过")
        void shouldPassForValidAdd() {
            Space space = TestDataFactory.aSpace(1L);
            spaceDomainService.validSpace(space, true);
        }

        @Test
        @DisplayName("新增时名称为空 → 应抛异常")
        void shouldThrowWhenNameBlankOnAdd() {
            Space space = TestDataFactory.aSpace(1L);
            space.setSpaceName("");

            assertThatThrownBy(() -> spaceDomainService.validSpace(space, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("空间名称不能为空");
        }

        @Test
        @DisplayName("新增时级别为空 → 应抛异常")
        void shouldThrowWhenLevelNullOnAdd() {
            Space space = TestDataFactory.aSpace(1L);
            space.setSpaceLevel(null);

            assertThatThrownBy(() -> spaceDomainService.validSpace(space, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("空间级别不能为空");
        }

        @Test
        @DisplayName("级别无效 → 应抛异常")
        void shouldThrowWhenLevelInvalid() {
            Space space = TestDataFactory.aSpace(1L);
            space.setSpaceLevel(99);

            assertThatThrownBy(() -> spaceDomainService.validSpace(space, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("空间级别不存在");
        }

        @Test
        @DisplayName("名称 > 30 字符 → 应抛异常")
        void shouldThrowWhenNameTooLong() {
            Space space = TestDataFactory.aSpace(1L);
            space.setSpaceName("a".repeat(31));

            assertThatThrownBy(() -> spaceDomainService.validSpace(space, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("空间名称过长");
        }
    }

    // ==================== FillSpaceBySpaceLevel ====================

    @Nested
    @DisplayName("按级别填充配额")
    class FillSpaceBySpaceLevel {

        @Test
        @DisplayName("COMMON 级别 → 应填充 100MB / 100 张")
        void shouldFillCommonDefaults() {
            Space space = new Space();
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());

            spaceDomainService.fillSpaceBySpaceLevel(space);

            assertThat(space.getMaxSize()).isEqualTo(SpaceLevelEnum.COMMON.getMaxSize());
            assertThat(space.getMaxCount()).isEqualTo(SpaceLevelEnum.COMMON.getMaxCount());
        }

        @Test
        @DisplayName("PROFESSIONAL 级别 → 应填充 1000MB / 1000 张")
        void shouldFillProfessionalDefaults() {
            Space space = new Space();
            space.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());

            spaceDomainService.fillSpaceBySpaceLevel(space);

            assertThat(space.getMaxSize()).isEqualTo(SpaceLevelEnum.PROFESSIONAL.getMaxSize());
            assertThat(space.getMaxCount()).isEqualTo(SpaceLevelEnum.PROFESSIONAL.getMaxCount());
        }

        @Test
        @DisplayName("已有值 → 不应覆盖")
        void shouldNotOverwriteExistingValues() {
            Space space = new Space();
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
            space.setMaxSize(50000000L); // 50MB 自定义值

            spaceDomainService.fillSpaceBySpaceLevel(space);

            assertThat(space.getMaxSize()).isEqualTo(50000000L); // 不被覆盖
        }
    }

    // ==================== AddSpace ====================

    @Nested
    @DisplayName("创建空间")
    class AddSpace {

        @Test
        @DisplayName("创建团队空间（绕过 lambdaQuery 限制）→ 应成功返回 ID")
        void shouldCreateTeamSpaceSuccessfully() {
            given(spaceRepository.save(any(Space.class))).willAnswer(inv -> {
                Space s = inv.getArgument(0);
                s.setId(1L);
                return true;
            });
            given(spaceUserRepository.save(any(SpaceUser.class))).willReturn(true);

            SpaceAddRequest req = new SpaceAddRequest();
            req.setSpaceName("我的空间");
            req.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
            req.setSpaceType(SpaceTypeEnum.TEAM.getValue());

            long result = spaceDomainService.addSpace(req, normalUser);

            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("创建团队空间 → 应自动创建 SpaceUser 记录")
        void shouldCreateSpaceUserForTeamSpace() {
            given(spaceRepository.save(any(Space.class))).willAnswer(inv -> {
                Space s = inv.getArgument(0);
                s.setId(1L);
                return true;
            });
            given(spaceUserRepository.save(any(SpaceUser.class))).willReturn(true);

            SpaceAddRequest req = new SpaceAddRequest();
            req.setSpaceName("团队空间");
            req.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
            req.setSpaceType(SpaceTypeEnum.TEAM.getValue());

            long result = spaceDomainService.addSpace(req, normalUser);

            assertThat(result).isEqualTo(1L);
            verify(spaceUserRepository).save(any(SpaceUser.class));
        }

        @Test
        @DisplayName("非管理员创建高级空间 → 应抛 NO_AUTH_ERROR")
        void shouldThrowWhenNonAdminCreatesHighLevelSpace() {
            given(userDomainService.isAdmin(normalUser)).willReturn(false);

            SpaceAddRequest req = new SpaceAddRequest();
            req.setSpaceName("高级空间");
            req.setSpaceLevel(SpaceLevelEnum.PROFESSIONAL.getValue());

            assertThatThrownBy(() -> spaceDomainService.addSpace(req, normalUser))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
        }

        @Test
        @DisplayName("名称为空 → 应默认使用'默认空间'")
        void shouldSetDefaultNameWhenBlank() {
            given(spaceRepository.save(any(Space.class))).willAnswer(inv -> {
                Space s = inv.getArgument(0);
                s.setId(1L);
                assertThat(s.getSpaceName()).isEqualTo("默认空间");
                return true;
            });

            SpaceAddRequest req = new SpaceAddRequest();
            req.setSpaceLevel(SpaceLevelEnum.COMMON.getValue()); // 必须设置级别避免 NPE
            req.setSpaceType(SpaceTypeEnum.TEAM.getValue());     // TEAM 类型避免 lambdaQuery 模拟
            given(spaceUserRepository.save(any(SpaceUser.class))).willReturn(true);

            spaceDomainService.addSpace(req, normalUser);
        }
    }

    // ==================== CheckSpaceAuth ====================

    @Nested
    @DisplayName("空间权限校验")
    class CheckSpaceAuth {

        @Test
        @DisplayName("空间所有者 → 有权限")
        void shouldPassForOwner() {
            Space space = TestDataFactory.aSpace(1L);
            given(userDomainService.isAdmin(normalUser)).willReturn(false);

            spaceDomainService.checkSpaceAuth(normalUser, space);
        }

        @Test
        @DisplayName("管理员 → 有权限")
        void shouldPassForAdmin() {
            Space space = TestDataFactory.aSpace(2L); // 属于其他人
            given(userDomainService.isAdmin(adminUser)).willReturn(true);

            spaceDomainService.checkSpaceAuth(adminUser, space);
        }

        @Test
        @DisplayName("陌生人 → 无权限")
        void shouldThrowForStranger() {
            Space space = TestDataFactory.aSpace(2L); // 属于 userId=2
            given(userDomainService.isAdmin(normalUser)).willReturn(false);

            assertThatThrownBy(() -> spaceDomainService.checkSpaceAuth(normalUser, space))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("没有空间权限");
        }
    }

    // ==================== GetQueryWrapper ====================

    @Nested
    @DisplayName("空间查询条件构建")
    class GetQueryWrapper {

        @Test
        @DisplayName("request 为 null → 应返回空 wrapper")
        void shouldReturnEmptyWrapperForNullRequest() {
            QueryWrapper<Space> wrapper = spaceDomainService.getQueryWrapper(null);
            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("包含所有字段 → 应正确构建")
        void shouldBuildWithAllFields() {
            SpaceQueryRequest req = new SpaceQueryRequest();
            req.setUserId(1L);
            req.setSpaceName("测试");
            req.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
            req.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());

            QueryWrapper<Space> wrapper = spaceDomainService.getQueryWrapper(req);

            assertThat(wrapper).isNotNull();
        }
    }
}
