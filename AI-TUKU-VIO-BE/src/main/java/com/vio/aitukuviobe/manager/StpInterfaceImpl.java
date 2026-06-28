package com.vio.aitukuviobe.manager;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.vio.aitukuviobe.constant.UserConstant;
import com.vio.aitukuviobe.exception.BusinessException;
import com.vio.aitukuviobe.exception.ErrorCode;
import com.vio.aitukuviobe.model.dto.spaceuser.SpaceUserAuthContext;
import com.vio.aitukuviobe.model.entity.Picture;
import com.vio.aitukuviobe.model.entity.Space;
import com.vio.aitukuviobe.model.entity.SpaceUser;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.enums.SpaceRoleEnum;
import com.vio.aitukuviobe.model.enums.SpaceTypeEnum;
import com.vio.aitukuviobe.service.PictureService;
import com.vio.aitukuviobe.service.SpaceService;
import com.vio.aitukuviobe.service.SpaceUserService;
import com.vio.aitukuviobe.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.vio.aitukuviobe.constant.SpaceUserPermissionConstant.PICTURE_VIEW;

@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private PictureService pictureService;
    @Resource
    private UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        if (!StpKit.SPACE_TYPE.equals(loginType)) return new ArrayList<>();

        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(
                SpaceRoleEnum.ADMIN.getValue());
        SpaceUserAuthContext authContext = getAuthContextByRequest();

        if (isAllFieldsNull(authContext)) return ADMIN_PERMISSIONS;

        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId)
                .get(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        Long userId = loginUser.getId();

        // 优先从上下文获取 spaceUser
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null)
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());

        // 通过 spaceUserId 查询
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId).one();
            if (loginSpaceUser == null) return new ArrayList<>();
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        // 通过 spaceId 或 pictureId 获取空间信息
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) return ADMIN_PERMISSIONS;
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId).one();
            if (picture == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
            spaceId = picture.getSpaceId();
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser))
                    return ADMIN_PERMISSIONS;
                else return Collections.singletonList(PICTURE_VIEW);
            }
        }

        Space space = spaceService.getById(spaceId);
        if (space == null) throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);

        if (space.getSpaceType() == null
                || SpaceTypeEnum.PRIVATE.getValue() == space.getSpaceType()) {
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser))
                return ADMIN_PERMISSIONS;
            else return new ArrayList<>();
        } else {
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId).one();
            if (spaceUser == null) return new ArrayList<>();
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    private boolean isAllFieldsNull(SpaceUserAuthContext context) {
        return context.getPictureId() == null
                && context.getSpaceId() == null
                && context.getSpaceUserId() == null
                && context.getSpaceUser() == null
                && context.getPicture() == null
                && context.getSpace() == null;
    }

    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = cn.hutool.extra.servlet.ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            authRequest = cn.hutool.core.bean.BeanUtil.toBean(
                    cn.hutool.extra.servlet.ServletUtil.getParamMap(request),
                    SpaceUserAuthContext.class);
        }
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String contextPath = request.getContextPath();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = cn.hutool.core.util.StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":   authRequest.setPictureId(id); break;
                case "spaceUser": authRequest.setSpaceUserId(id); break;
                case "space":     authRequest.setSpaceId(id); break;
            }
        }
        return authRequest;
    }
}
