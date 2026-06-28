package com.vio.aitukuviobe.manager.websocket.disruptor;

import com.lmax.disruptor.WorkHandler;
import com.vio.aitukuviobe.manager.websocket.PictureEditHandler;
import com.vio.aitukuviobe.manager.websocket.model.PictureEditMessageTypeEnum;
import com.vio.aitukuviobe.manager.websocket.model.PictureEditRequestMessage;
import com.vio.aitukuviobe.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    private PictureEditHandler pictureEditHandler;

    @Override
    public void onEvent(PictureEditEvent event) {
        PictureEditRequestMessage msg = event.getPictureEditRequestMessage();
        WebSocketSession session = event.getSession();
        User user = event.getUser();
        Long pictureId = event.getPictureId();
        PictureEditMessageTypeEnum type = PictureEditMessageTypeEnum.getEnumByValue(msg.getType());
        if (type == null) return;
        try {
            switch (type) {
                case ENTER_EDIT:
                    pictureEditHandler.handleEnterEditMessage(msg, session, user, pictureId); break;
                case EDIT_ACTION:
                    pictureEditHandler.handleEditActionMessage(msg, session, user, pictureId); break;
                case EXIT_EDIT:
                    pictureEditHandler.handleExitEditMessage(msg, session, user, pictureId); break;
            }
        } catch (Exception e) {
            log.error("处理编辑事件失败", e);
        }
    }
}
