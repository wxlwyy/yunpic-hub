package com.wyy.yunpicturebackend.manager.websocket.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.wyy.yunpicturebackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;

@Component
@Slf4j
public class PictureEditEventProducer {

    @Resource
    Disruptor<PictureEditEvent> pictureEditEventDisruptor;

    public void publishEvent(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                             Long pictureId, User user) {
        // RingBuffer 是 Disruptor 的核心数据结构（一个环形、固定大小的数组）所有事件都存放在这个 buffer 里
        RingBuffer<PictureEditEvent> ringBuffer = pictureEditEventDisruptor.getRingBuffer();
        // next() 会返回下一个可写的位置索引,如果队列满了，这里会阻塞
        long next = ringBuffer.next();
        // 是从预分配的对象池中复用已有对象（在 Config 里用 PictureEditEvent::new 初始化了整个 buffer）,避免频繁 GC，这是 Disruptor 高性能的核心之一
        PictureEditEvent pictureEditEvent = ringBuffer.get(next);
        pictureEditEvent.setPictureEditRequestMessage(pictureEditRequestMessage);
        pictureEditEvent.setSession(session);
        pictureEditEvent.setUser(user);
        pictureEditEvent.setPictureId(pictureId);
        // 发布事件，告诉 Disruptor：“这个位置的数据已经准备好了，消费者可以处理了”消费者线程会立即（或尽快）从这个位置读取事件
        ringBuffer.publish(next);
    }

    /**
     * 优雅停机（等事件处理完再关线程，避免 JVM 残留）
     */
    /* @PreDestroy
    public void close() {
        pictureEditEventDisruptor.shutdown();
    } */
}