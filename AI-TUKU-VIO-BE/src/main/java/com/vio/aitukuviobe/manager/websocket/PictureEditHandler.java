package com.vio.aitukuviobe.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.vio.aitukuviobe.manager.websocket.disruptor.PictureEditEventProducer;
import com.vio.aitukuviobe.manager.websocket.model.PictureEditActionEnum;
import com.vio.aitukuviobe.manager.websocket.model.PictureEditMessageTypeEnum;
import com.vio.aitukuviobe.manager.websocket.model.PictureEditRequestMessage;
import com.vio.aitukuviobe.manager.websocket.model.PictureEditResponseMessage;
import com.vio.aitukuviobe.model.entity.User;
import com.vio.aitukuviobe.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    // 每张图片的编辑状态，key: pictureId, value: 当前正在编辑的用户 ID
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    // 保存所有连接的会话，key: pictureId, value: 用户会话集合
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    private void broadcastToPicture(Long pictureId,
            PictureEditResponseMessage pictureEditResponseMessage,
            WebSocketSession excludeSession) throws Exception {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            for (WebSocketSession session : sessionSet) {
                if (excludeSession != null && excludeSession.equals(session)) continue;
                if (session.isOpen()) session.sendMessage(textMessage);
            }
        }
    }

    private void broadcastToPicture(Long pictureId,
            PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);

        PictureEditResponseMessage resp = new PictureEditResponseMessage();
        resp.setType(PictureEditMessageTypeEnum.INFO.getValue());
        resp.setMessage(String.format("%s加入编辑", user.getUserName()));
        resp.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, resp);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session,
            TextMessage message) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage =
                JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);
        Map<String, Object> attributes = session.getAttributes();
        User user = (User) attributes.get("user");
        Long pictureId = (Long) attributes.get("pictureId");
        // 通过 Disruptor 异步处理消息
        pictureEditEventProducer.publishEvent(
                pictureEditRequestMessage, session, user, pictureId);
    }

    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
            WebSocketSession session, User user, Long pictureId) throws Exception {
        if (!pictureEditingUsers.containsKey(pictureId)) {
            pictureEditingUsers.put(pictureId, user.getId());
            PictureEditResponseMessage resp = new PictureEditResponseMessage();
            resp.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            resp.setMessage(String.format("%s开始编辑图片", user.getUserName()));
            resp.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, resp);
        }
    }

    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage,
            WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum actionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if (actionEnum == null) return;
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            PictureEditResponseMessage resp = new PictureEditResponseMessage();
            resp.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            resp.setMessage(String.format("%s执行%s", user.getUserName(), actionEnum.getText()));
            resp.setEditAction(editAction);
            resp.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, resp, session);
        }
    }

    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage,
            WebSocketSession session, User user, Long pictureId) throws Exception {
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            pictureEditingUsers.remove(pictureId);
            PictureEditResponseMessage resp = new PictureEditResponseMessage();
            resp.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            resp.setMessage(String.format("%s退出编辑图片", user.getUserName()));
            resp.setUser(userService.getUserVO(user));
            broadcastToPicture(pictureId, resp);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session,
            CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        handleExitEditMessage(null, session, user, pictureId);
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) pictureSessions.remove(pictureId);
        }
        PictureEditResponseMessage resp = new PictureEditResponseMessage();
        resp.setType(PictureEditMessageTypeEnum.INFO.getValue());
        resp.setMessage(String.format("%s离开编辑", user.getUserName()));
        resp.setUser(userService.getUserVO(user));
        broadcastToPicture(pictureId, resp);
    }
}
