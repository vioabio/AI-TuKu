package com.vio.aitukuviobe.shared.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.vio.aitukuviobe.shared.websocket.model.PictureEditRequestMessage;
import com.vio.aitukuviobe.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    private Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    public void publishEvent(PictureEditRequestMessage message,
            WebSocketSession session, User user, Long pictureId) {
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        long next = ringBuffer.next();
        PictureEditEvent event = ringBuffer.get(next);
        event.setSession(session);
        event.setPictureEditRequestMessage(message);
        event.setUser(user);
        event.setPictureId(pictureId);
        ringBuffer.publish(next);
    }

    @PreDestroy
    public void close() {
        pictureEditEventDisruptor.shutdown();
    }
}
