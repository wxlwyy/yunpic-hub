package com.wyy.yunpicturebackend.manager.websocket.disruptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.wyy.yunpicturebackend.model.entity.User;

import com.lmax.disruptor.WorkHandler;
import com.wyy.yunpicturebackend.manager.websocket.PictureEditHandler;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.wyy.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.wyy.yunpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum.*;

@Component
@Slf4j
public class PictureEditEventWorkHandler implements WorkHandler<PictureEditEvent> {

    @Resource
    @Lazy
    private PictureEditHandler pictureEditHandler;

    @Resource
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void onEvent(PictureEditEvent pictureEditEvent) throws Exception {
        PictureEditRequestMessage pictureEditRequestMessage = pictureEditEvent.getPictureEditRequestMessage();
        WebSocketSession session = pictureEditEvent.getSession();
        User user = pictureEditEvent.getUser();
        Long pictureId = pictureEditEvent.getPictureId();

        String pictureEditRequestMessageType = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = getEnumByValue(pictureEditRequestMessageType);

        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                pictureEditHandler.handleEnterEditMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            case EDIT_ACTION:
                pictureEditHandler.handleEditActionMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            case EXIT_EDIT:
                pictureEditHandler.handleExitEditMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUserVO(userService.getUserVO(user));
                String pictureEditResponseMessageJson = objectMapper.writeValueAsString(pictureEditResponseMessage);
                TextMessage textMessage = new TextMessage(pictureEditResponseMessageJson);
                session.sendMessage(textMessage);
        }
    }
}
