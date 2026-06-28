package com.vio.aitukuviobe.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.vio.aitukuviobe.constant.SpaceUserPermissionConstant;
import com.vio.aitukuviobe.manager.SpaceUserAuthManager;
import com.vio.aitukuviobe.model.entity.Picture;
import com.vio.aitukuviobe.model.entity.Space;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.model.enums.SpaceTypeEnum;
import com.vio.aitukuviobe.service.PictureService;
import com.vio.aitukuviobe.service.SpaceService;
import com.vio.aitukuviobe.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;
    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
            ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest =
                    ((ServletServerHttpRequest) request).getServletRequest();
            String pictureId = servletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            User loginUser = userService.getLoginUser(servletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            Picture picture = pictureService.getById(pictureId);
            if (picture == null) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() == null
                        || space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.info("非团队空间，拒绝握手");
                    return false;
                }
            } else {
                // 公共图库无空间，不需要协同编辑
                log.info("公共图库图片，拒绝握手");
                return false;
            }
            List<String> permissionList =
                    spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有图片编辑权限，拒绝握手");
                return false;
            }
            attributes.put("user", loginUser);
            attributes.put("userId", loginUser.getId());
            attributes.put("pictureId", Long.valueOf(pictureId));
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
            ServerHttpResponse response, WebSocketHandler wsHandler,
            Exception exception) {
    }
}
