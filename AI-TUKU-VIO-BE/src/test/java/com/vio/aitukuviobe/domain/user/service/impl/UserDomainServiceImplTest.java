package com.vio.aitukuviobe.domain.user.service.impl;

import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.valueobject.UserRoleEnum;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDomainService 单元测试")
class UserDomainServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDomainServiceImpl userDomainService;

    @Nested
    @DisplayName("用户注册")
    class UserRegister {

        @Test
        @DisplayName("正常注册 → 密码加密存储")
        void shouldRegisterWithEncryptedPassword() {
            String rawPassword = "12345678";
            given(userRepository.count(any())).willReturn(0L);
            given(userRepository.save(any(User.class))).willReturn(true);

            userDomainService.userRegister("testUser", rawPassword, rawPassword);

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("重复账号注册 → 抛异常")
        void shouldThrowWhenAccountExists() {
            given(userRepository.count(any())).willReturn(1L);

            assertThatThrownBy(() ->
                userDomainService.userRegister("existing", "12345678", "12345678"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("账号重复");
        }

        @Test
        @DisplayName("密码不足8位 → 抛异常")
        void shouldThrowWhenPasswordTooShort() {
            assertThatThrownBy(() ->
                userDomainService.userRegister("user", "123", "123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码过短");
        }

        @Test
        @DisplayName("两次密码不一致 → 抛异常")
        void shouldThrowWhenPasswordsNotMatch() {
            assertThatThrownBy(() ->
                userDomainService.userRegister("user", "12345678", "87654321"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("密码不一致");
        }
    }

    @Nested
    @DisplayName("密码加密")
    class PasswordEncryption {

        @Test
        @DisplayName("应返回 MD5 加密后的密码")
        void shouldReturnMd5EncryptedPassword() {
            String encrypted = userDomainService.getEncryptPassword("12345678");

            assertThat(encrypted).isNotBlank();
            assertThat(encrypted).isNotEqualTo("12345678");
        }
    }

    @Nested
    @DisplayName("管理员判断")
    class AdminCheck {

        @Test
        @DisplayName("admin 角色 → isAdmin 返回 true")
        void adminRoleShouldReturnTrue() {
            User admin = TestDataFactory.aUser().userRole(UserRoleEnum.ADMIN.getValue()).build();
            assertThat(userDomainService.isAdmin(admin)).isTrue();
        }

        @Test
        @DisplayName("普通用户 → isAdmin 返回 false")
        void userRoleShouldReturnFalse() {
            User user = TestDataFactory.aUser().userRole(UserRoleEnum.USER.getValue()).build();
            assertThat(userDomainService.isAdmin(user)).isFalse();
        }

        @Test
        @DisplayName("null 用户 → isAdmin 返回 false")
        void nullUserShouldReturnFalse() {
            assertThat(userDomainService.isAdmin(null)).isFalse();
        }
    }
}
