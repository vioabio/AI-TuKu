package com.vio.aitukuviobe.infrastructure.oauth;

import cn.hutool.core.util.StrUtil;
import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.repository.UserRepository;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.domain.user.valueobject.UserRoleEnum;
import com.vio.aitukuviobe.infrastructure.config.OAuth2Config;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.shared.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * OAuth2 第三方登录服务
 * <p>
 * 支持：GitHub、Google、微信开放平台
 * 登录流程：前端跳转授权页 → 回调 → JustAuth 获取用户信息 → 自动注册/登录 → Sa-Token 签发会话
 *
 * @author vivin
 */
@Slf4j
@Service
public class OAuth2Service {

    @Resource
    private OAuth2Config oAuth2Config;

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserDomainService userDomainService;

    /**
     * 获取第三方授权 URL（前端跳转到此地址发起授权）
     *
     * @param platform 平台标识：github / google / wechat
     * @return 授权 URL
     */
    public String getAuthorizeUrl(String platform) {
        AuthRequest authRequest = getAuthRequest(platform);
        return authRequest.authorize();
    }

    /**
     * OAuth2 回调处理：获取用户信息 → 自动注册/登录
     *
     * @param platform 平台标识
     * @param callback 授权回调参数（code + state）
     * @param request  HTTP 请求
     * @return 登录后的用户信息
     */
    public User handleCallback(String platform, AuthCallback callback, HttpServletRequest request) {
        AuthRequest authRequest = getAuthRequest(platform);

        // 通过 JustAuth 获取第三方用户信息
        AuthResponse<AuthUser> response = authRequest.login(callback);
        if (!response.ok()) {
            log.error("OAuth2 登录失败: platform={}, msg={}", platform, response.getMsg());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "第三方登录失败: " + response.getMsg());
        }

        AuthUser authUser = response.getData();
        return loginOrRegister(authUser, platform, request);
    }

    /**
     * 自动注册或登录
     * <p>
     * 规则：根据 platform + uuid 查找已绑定账号 → 找到则登录，未找到则自动注册新账号
     */
    private User loginOrRegister(AuthUser authUser, String platform, HttpServletRequest request) {
        // 生成唯一账号标识（平台名 + UUID 前8位）
        String userAccount = platform + "_" + authUser.getUuid().substring(0, 8);

        // 查找是否已有该第三方账号绑定的用户
        User user = userRepository.lambdaQuery()
                .eq(User::getUserAccount, userAccount)
                .one();

        if (user == null) {
            // 自动注册
            user = new User();
            user.setUserAccount(userAccount);
            // 第三方登录无需密码，生成随机加密密码
            user.setUserPassword(userDomainService.getEncryptPassword(authUser.getUuid()));
            user.setUserName(StrUtil.blankToDefault(authUser.getNickname(), platform + "_用户"));
            user.setUserAvatar(StrUtil.blankToDefault(authUser.getAvatar(), ""));
            user.setUserProfile(authUser.getRemark());
            user.setUserRole(UserRoleEnum.USER.getValue());
            boolean saved = userRepository.save(user);
            if (!saved) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "第三方账号注册失败");
            }
            log.info("OAuth2 自动注册: platform={}, uuid={}, userAccount={}",
                    platform, authUser.getUuid(), userAccount);
        }

        // 登录：设置 Sa-Token 会话
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set("loginUser", user);
        request.getSession().setAttribute("userLoginState", user);

        log.info("OAuth2 登录成功: platform={}, userId={}", platform, user.getId());
        return user;
    }

    /**
     * 根据平台创建对应的 AuthRequest
     */
    private AuthRequest getAuthRequest(String platform) {
        OAuth2Config.OAuth2PlatformConfig config = oAuth2Config.getPlatforms().get(platform);
        if (config == null || StrUtil.isBlank(config.getClientId())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "未配置 " + platform + " 的 OAuth2 参数，请在 application.yml 中设置");
        }

        AuthConfig authConfig = AuthConfig.builder()
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .redirectUri(config.getRedirectUri())
                .build();

        switch (platform.toLowerCase()) {
            case "github":
                return new AuthGithubRequest(authConfig);
            case "google":
                return new AuthGoogleRequest(authConfig);
            case "wechat":
                return new AuthWeChatOpenRequest(authConfig);
            case "gitee":
                return new AuthGiteeRequest(authConfig);
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "不支持的 OAuth2 平台: " + platform);
        }
    }
}
