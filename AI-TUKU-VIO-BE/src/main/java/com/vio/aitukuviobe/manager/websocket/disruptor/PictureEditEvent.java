package com.vio.aitukuviobe.manager.websocket.disruptor;

import com.vio.aitukuviobe.manager.websocket.model.PictureEditRequestMessage;
import com.vio.aitukuviobe.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {
    private PictureEditRequestMessage pictureEditRequestMessage;
    private WebSocketSession session;
    private User user;
    private Long pictureId;
}
