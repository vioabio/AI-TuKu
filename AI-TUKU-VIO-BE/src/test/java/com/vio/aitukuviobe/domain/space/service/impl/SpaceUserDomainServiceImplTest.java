package com.vio.aitukuviobe.domain.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.space.entity.Space;
import com.vio.aitukuviobe.domain.space.entity.SpaceUser;
import com.vio.aitukuviobe.domain.space.repository.SpaceRepository;
import com.vio.aitukuviobe.domain.space.repository.SpaceUserRepository;
import com.vio.aitukuviobe.domain.space.valueobject.SpaceRoleEnum;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.vio.aitukuviobe.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.vio.aitukuviobe.testbase.BaseUnitTest;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("SpaceUserDomainServiceImpl 单元测试")
class SpaceUserDomainServiceImplTest extends BaseUnitTest {

    @Mock
    private SpaceUserRepository spaceUserRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SpaceRepository spaceRepository;

    @InjectMocks
    private SpaceUserDomainServiceImpl spaceUserDomainService;

    // ==================== ValidSpaceUser ====================

    @Nested
    @DisplayName("空间成员校验")
    class ValidSpaceUser {

        @Test
        @DisplayName("新增时有效成员 → 应通过")
        void shouldPassForValidAdd() {
            given(userRepository.getById(1L)).willReturn(TestDataFactory.aUser());
            given(spaceRepository.getById(1L)).willReturn(TestDataFactory.aSpace(1L));

            SpaceUser spaceUser = TestDataFactory.aSpaceUser(1L, 1L);

            spaceUserDomainService.validSpaceUser(spaceUser, true);
        }

        @Test
        @DisplayName("spaceId 或 userId 为空 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenSpaceIdOrUserIdNull() {
            SpaceUser spaceUser = new SpaceUser();

            assertThatThrownBy(() -> spaceUserDomainService.validSpaceUser(spaceUser, true))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("用户不存在 → 应抛 NOT_FOUND_ERROR")
        void shouldThrowWhenUserNotFound() {
            given(userRepository.getById(1L)).willReturn(null);
            SpaceUser spaceUser = TestDataFactory.aSpaceUser(1L, 1L);

            assertThatThrownBy(() -> spaceUserDomainService.validSpaceUser(spaceUser, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在");
        }

        @Test
        @DisplayName("空间不存在 → 应抛 NOT_FOUND_ERROR")
        void shouldThrowWhenSpaceNotFound() {
            given(userRepository.getById(1L)).willReturn(TestDataFactory.aUser());
            given(spaceRepository.getById(1L)).willReturn(null);
            SpaceUser spaceUser = TestDataFactory.aSpaceUser(1L, 1L);

            assertThatThrownBy(() -> spaceUserDomainService.validSpaceUser(spaceUser, true))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("空间不存在");
        }

        @Test
        @DisplayName("角色无效 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenInvalidRole() {
            SpaceUser spaceUser = TestDataFactory.aSpaceUser(1L, 1L);
            spaceUser.setSpaceRole("owner"); // 无效角色
            // add=false 时不会校验 user/space 存在

            assertThatThrownBy(() -> spaceUserDomainService.validSpaceUser(spaceUser, false))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("空间角色不存在");
        }
    }

    // ==================== AddSpaceUser ====================

    @Nested
    @DisplayName("添加空间成员")
    class AddSpaceUser {

        @Test
        @DisplayName("正常添加 → 应返回 ID")
        void shouldAddSpaceUserSuccessfully() {
            given(userRepository.getById(1L)).willReturn(TestDataFactory.aUser());
            given(spaceRepository.getById(1L)).willReturn(TestDataFactory.aSpace(1L));
            given(spaceUserRepository.save(any(SpaceUser.class))).willAnswer(inv -> {
                SpaceUser su = inv.getArgument(0);
                su.setId(1L);
                return true;
            });

            SpaceUserAddRequest req = new SpaceUserAddRequest();
            req.setSpaceId(1L);
            req.setUserId(1L);
            req.setSpaceRole(SpaceRoleEnum.EDITOR.getValue());

            long result = spaceUserDomainService.addSpaceUser(req);

            assertThat(result).isEqualTo(1L);
        }
    }

    // ==================== GetQueryWrapper ====================

    @Nested
    @DisplayName("查询条件构建")
    class GetQueryWrapper {

        @Test
        @DisplayName("request 为 null → 应返回空 wrapper")
        void shouldReturnEmptyWrapperForNullRequest() {
            QueryWrapper<SpaceUser> wrapper = spaceUserDomainService.getQueryWrapper(null);
            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("包含所有字段 → 应正确构建")
        void shouldBuildWithAllFields() {
            SpaceUserQueryRequest req = new SpaceUserQueryRequest();
            req.setSpaceId(1L);
            req.setUserId(1L);
            req.setSpaceRole(SpaceRoleEnum.EDITOR.getValue());

            QueryWrapper<SpaceUser> wrapper = spaceUserDomainService.getQueryWrapper(req);

            assertThat(wrapper).isNotNull();
        }
    }
}
