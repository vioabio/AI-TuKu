package com.vio.aitukuviobe.domain.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.valueobject.UserRoleEnum;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.interfaces.dto.user.UserQueryRequest;
import com.vio.aitukuviobe.interfaces.vo.LoginUserVO;
import com.vio.aitukuviobe.testbase.BaseUnitTest;
import com.vio.aitukuviobe.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("UserDomainServiceImpl 单元测试")
class UserDomainServiceImplTest extends BaseUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @InjectMocks
    private UserDomainServiceImpl userDomainService;

    private static final String VALID_ACCOUNT = "testuser";
    private static final String VALID_PASSWORD = "12345678";
    // 动态计算加密密码，与服务端逻辑保持一致
    private static final String ENCRYPTED_PASSWORD =
            DigestUtils.md5DigestAsHex(("vio" + "12345678").getBytes());

    private void mockSession() {
        given(request.getSession()).willReturn(session);
    }

    // ==================== Register ====================

    @Nested
    @DisplayName("用户注册")
    class Register {

        @Test
        @DisplayName("正常注册 → 应返回用户 ID，密码已加密存储")
        void shouldRegisterSuccessfully() {
            given(userRepository.count(any(QueryWrapper.class))).willReturn(0L);
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return true;
            });

            long userId = userDomainService.userRegister(VALID_ACCOUNT, VALID_PASSWORD, VALID_PASSWORD);

            assertThat(userId).isEqualTo(1L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("账号为空 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenAccountBlank() {
            assertThatThrownBy(() -> userDomainService.userRegister("", VALID_PASSWORD, VALID_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("账号 < 4 位 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenAccountTooShort() {
            assertThatThrownBy(() -> userDomainService.userRegister("ab", VALID_PASSWORD, VALID_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户账号过短");
        }

        @Test
        @DisplayName("密码 < 8 位 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenPasswordTooShort() {
            assertThatThrownBy(() -> userDomainService.userRegister(VALID_ACCOUNT, "1234567", "1234567"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户密码过短");
        }

        @Test
        @DisplayName("两次密码不一致 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenPasswordsMismatch() {
            assertThatThrownBy(() -> userDomainService.userRegister(VALID_ACCOUNT, VALID_PASSWORD, "different"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("两次输入的密码不一致");
        }

        @Test
        @DisplayName("账号已存在 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenAccountExists() {
            given(userRepository.count(any(QueryWrapper.class))).willReturn(1L);

            assertThatThrownBy(() -> userDomainService.userRegister(VALID_ACCOUNT, VALID_PASSWORD, VALID_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("账号重复");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("数据库保存失败 → 应抛 SYSTEM_ERROR")
        void shouldThrowWhenSaveFails() {
            given(userRepository.count(any(QueryWrapper.class))).willReturn(0L);
            given(userRepository.save(any(User.class))).willReturn(false);

            assertThatThrownBy(() -> userDomainService.userRegister(VALID_ACCOUNT, VALID_PASSWORD, VALID_PASSWORD))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.SYSTEM_ERROR.getCode());
        }

        @Test
        @DisplayName("注册成功后密码应为 MD5 加密格式")
        void shouldStoreEncryptedPassword() {
            given(userRepository.count(any(QueryWrapper.class))).willReturn(0L);
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User savedUser = inv.getArgument(0);
                savedUser.setId(1L);
                // 验证密码不等于明文
                assertThat(savedUser.getUserPassword()).isNotEqualTo(VALID_PASSWORD);
                assertThat(savedUser.getUserPassword()).isEqualTo(ENCRYPTED_PASSWORD);
                return true;
            });

            userDomainService.userRegister(VALID_ACCOUNT, VALID_PASSWORD, VALID_PASSWORD);
        }
    }

    // ==================== Login ====================

    @Nested
    @DisplayName("用户登录")
    class Login {

        @Test
        @DisplayName("正常登录 → 应返回 LoginUserVO")
        void shouldLoginSuccessfully() {
            mockSession();
            User mockUser = TestDataFactory.aUser();
            mockUser.setId(1L);
            mockUser.setUserAccount(VALID_ACCOUNT);
            mockUser.setUserPassword(ENCRYPTED_PASSWORD);
            given(userRepository.getOne(any(QueryWrapper.class))).willReturn(mockUser);

            LoginUserVO result = userDomainService.userLogin(VALID_ACCOUNT, VALID_PASSWORD, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(session).setAttribute(anyString(), any());
        }

        @Test
        @DisplayName("密码错误 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenWrongPassword() {
            given(userRepository.getOne(any(QueryWrapper.class))).willReturn(null);

            assertThatThrownBy(() -> userDomainService.userLogin(VALID_ACCOUNT, "wrongpassword", request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在或者密码错误");
        }

        @Test
        @DisplayName("账号不存在 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenAccountNotFound() {
            given(userRepository.getOne(any(QueryWrapper.class))).willReturn(null);

            assertThatThrownBy(() -> userDomainService.userLogin("nonexistent", VALID_PASSWORD, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("用户不存在或者密码错误");
        }

        @Test
        @DisplayName("参数为空 → 应抛 PARAMS_ERROR")
        void shouldThrowWhenBlankInput() {
            assertThatThrownBy(() -> userDomainService.userLogin("", "", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }
    }

    // ==================== Logout ====================

    @Nested
    @DisplayName("用户登出")
    class Logout {

        @BeforeEach
        void setUp() {
            mockSession();
        }

        @Test
        @DisplayName("正常登出 → 应返回 true")
        void shouldLogoutSuccessfully() {
            given(session.getAttribute(anyString())).willReturn(new Object());

            boolean result = userDomainService.userLogout(request);

            assertThat(result).isTrue();
            verify(session).removeAttribute(anyString());
        }

        @Test
        @DisplayName("未登录时登出 → 应抛 OPERATION_ERROR")
        void shouldThrowWhenNotLoggedIn() {
            given(session.getAttribute(anyString())).willReturn(null);

            assertThatThrownBy(() -> userDomainService.userLogout(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("未登录");
        }
    }

    // ==================== GetEncryptPassword ====================

    @Nested
    @DisplayName("密码加密")
    class GetEncryptPassword {

        @Test
        @DisplayName("应返回 MD5(salt + password) 加密结果")
        void shouldReturnMd5EncryptedPassword() {
            String encrypted = userDomainService.getEncryptPassword(VALID_PASSWORD);
            assertThat(encrypted).isEqualTo(ENCRYPTED_PASSWORD);
            assertThat(encrypted).hasSize(32); // MD5 输出为 32 位十六进制字符串
        }

        @Test
        @DisplayName("相同输入应产生相同输出")
        void shouldBeDeterministic() {
            String first = userDomainService.getEncryptPassword(VALID_PASSWORD);
            String second = userDomainService.getEncryptPassword(VALID_PASSWORD);
            assertThat(first).isEqualTo(second);
        }
    }

    // ==================== IsAdmin ====================

    @Nested
    @DisplayName("管理员判断")
    class IsAdmin {

        @Test
        @DisplayName("admin 角色 → 应返回 true")
        void shouldReturnTrueForAdmin() {
            User admin = TestDataFactory.anAdminUser();
            assertThat(userDomainService.isAdmin(admin)).isTrue();
        }

        @Test
        @DisplayName("普通 user 角色 → 应返回 false")
        void shouldReturnFalseForNormalUser() {
            User user = TestDataFactory.aUser();
            assertThat(userDomainService.isAdmin(user)).isFalse();
        }

        @Test
        @DisplayName("null → 应返回 false")
        void shouldReturnFalseForNull() {
            assertThat(userDomainService.isAdmin(null)).isFalse();
        }
    }

    // ==================== GetLoginUser ====================

    @Nested
    @DisplayName("获取登录用户")
    class GetLoginUser {

        @BeforeEach
        void setUp() {
            mockSession();
        }

        @Test
        @DisplayName("session 中存在有效用户 → 应返回最新用户信息")
        void shouldReturnUserFromSession() {
            User sessionUser = TestDataFactory.aUser();
            sessionUser.setId(1L);
            given(session.getAttribute(anyString())).willReturn(sessionUser);
            given(userRepository.getById(1L)).willReturn(sessionUser);

            User result = userDomainService.getLoginUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("session 中无用户 → 应抛 NOT_LOGIN_ERROR")
        void shouldThrowWhenSessionHasNoUser() {
            given(session.getAttribute(anyString())).willReturn(null);

            assertThatThrownBy(() -> userDomainService.getLoginUser(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
        }

        @Test
        @DisplayName("DB 中用户已删除 → 应抛 NOT_LOGIN_ERROR")
        void shouldThrowWhenUserDeletedInDb() {
            User sessionUser = TestDataFactory.aUser();
            sessionUser.setId(1L);
            given(session.getAttribute(anyString())).willReturn(sessionUser);
            given(userRepository.getById(1L)).willReturn(null);

            assertThatThrownBy(() -> userDomainService.getLoginUser(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.NOT_LOGIN_ERROR.getCode());
        }
    }

    // ==================== GetLoginUserVO / GetUserVO ====================

    @Nested
    @DisplayName("VO 转换")
    class VoConversion {

        @Test
        @DisplayName("getLoginUserVO → 应正确转换属性")
        void shouldConvertToLoginUserVO() {
            User user = TestDataFactory.aUser();
            user.setId(1L);

            LoginUserVO vo = userDomainService.getLoginUserVO(user);

            assertThat(vo).isNotNull();
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getUserAccount()).isEqualTo(user.getUserAccount());
        }

        @Test
        @DisplayName("getLoginUserVO(null) → 应返回 null")
        void shouldReturnNullLoginUserVO() {
            assertThat(userDomainService.getLoginUserVO(null)).isNull();
        }

        @Test
        @DisplayName("getUserVO(null) → 应返回 null")
        void shouldReturnNullUserVO() {
            assertThat(userDomainService.getUserVO(null)).isNull();
        }

        @Test
        @DisplayName("getUserVOList → 空列表应返回空列表")
        void shouldReturnEmptyListForNullInput() {
            assertThat(userDomainService.getUserVOList(null)).isEmpty();
        }
    }

    // ==================== GetQueryWrapper ====================

    @Nested
    @DisplayName("查询条件构建")
    class GetQueryWrapper {

        @Test
        @DisplayName("request 为 null → 应抛 PARAMS_ERROR")
        void shouldThrowWhenRequestIsNull() {
            assertThatThrownBy(() -> userDomainService.getQueryWrapper(null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
        }

        @Test
        @DisplayName("包含所有字段 → 应构建正确的查询条件")
        void shouldBuildWithAllFields() {
            UserQueryRequest req = new UserQueryRequest();
            req.setId(1L);
            req.setUserAccount("test");
            req.setUserName("测试");
            req.setUserRole(UserRoleEnum.USER.getValue());
            req.setSortField("createTime");
            req.setSortOrder("descend");

            QueryWrapper<User> wrapper = userDomainService.getQueryWrapper(req);

            assertThat(wrapper).isNotNull();
        }

        @Test
        @DisplayName("升序排序 → sortOrder 应为 ascend")
        void shouldHandleAscendingOrder() {
            UserQueryRequest req = new UserQueryRequest();
            req.setSortField("createTime");
            req.setSortOrder("ascend");

            // 不应抛异常
            QueryWrapper<User> wrapper = userDomainService.getQueryWrapper(req);
            assertThat(wrapper).isNotNull();
        }
    }
}
