package com.vio.aitukuviobe.interfaces.controller;

import com.vio.aitukuviobe.domain.user.entity.User;
import com.vio.aitukuviobe.domain.user.service.UserDomainService;
import com.vio.aitukuviobe.infrastructure.common.BaseResponse;
import com.vio.aitukuviobe.infrastructure.common.ResultUtils;
import com.vio.aitukuviobe.infrastructure.exception.BusinessException;
import com.vio.aitukuviobe.infrastructure.exception.ErrorCode;
import com.vio.aitukuviobe.infrastructure.exception.ThrowUtils;
import com.vio.aitukuviobe.infrastructure.oauth.OAuth2Service;
import com.vio.aitukuviobe.interfaces.vo.LoginUserVO;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.model.AuthCallback;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * OAuth2 第三方登录控制器
 * <p>
 * 登录流程：
 * 1. GET  /api/oauth/{platform}/authorize → 返回授权 URL（前端跳转）
 * 2. 用户在第三方平台授权
 * 3. 第三方回调 → GET /api/oauth/{platform}/callback?code=xxx&state=xxx
 * 4. 后端获取用户信息 → 自动注册/登录 → 返回 LoginUserVO
 *
 * @author vivin
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
public class OAuthController {

    @Resource
    private OAuth2Service oAuth2Service;

    @Resource
    private UserDomainService userDomainService;

    /**
     * 获取第三方授权 URL
     *
     * @param platform 平台标识：github / google / wechat
     * @return 授权 URL（前端通过 window.location.href 跳转）
     */
    @GetMapping("/{platform}/authorize")
    public BaseResponse<String> authorize(@PathVariable String platform) {
        ThrowUtils.throwIf(
                !platform.matches("^(github|google|wechat|gitee)$"),
                ErrorCode.PARAMS_ERROR, "不支持的平台: " + platform
        );
        String url = oAuth2Service.getAuthorizeUrl(platform);
        return ResultUtils.success(url);
    }

    /**
     * OAuth2 回调处理（第三方平台授权后回跳）
     *
     * @param platform 平台标识
     * @param code     授权码
     * @param state    防 CSRF 状态值
     * @param request  HTTP 请求
     * @return 登录用户信息
     */
    @GetMapping("/{platform}/callback")
    public BaseResponse<LoginUserVO> callback(
            @PathVariable String platform,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            HttpServletRequest request) {
        ThrowUtils.throwIf(
                !platform.matches("^(github|google|wechat|gitee)$"),
                ErrorCode.PARAMS_ERROR, "不支持的平台: " + platform
        );

        AuthCallback callback = AuthCallback.builder()
                .code(code)
                .state(state)
                .build();

        User user = oAuth2Service.handleCallback(platform, callback, request);
        LoginUserVO loginUserVO = userDomainService.getLoginUserVO(user);
        return ResultUtils.success(loginUserVO);
    }
}
