package com.vio.aitukuviobe.shared.websocket.disruptor;

import com.vio.aitukuviobe.shared.websocket.model.PictureEditRequestMessage;
import com.vio.aitukuviobe.domain.user.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {
    private PictureEditRequestMessage pictureEditRequestMessage;
    private WebSocketSession session;
    private User user;
    private Long pictureId;
}
