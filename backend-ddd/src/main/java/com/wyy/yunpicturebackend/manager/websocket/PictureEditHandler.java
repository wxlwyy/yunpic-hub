package com.wyy.yunpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wyy.yunpicturebackend.manager.websocket.disruptor.PictureEditEventProducer;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditActionEnum;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditMessageTypeEnum;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditRequestMessage;
import com.wyy.yunpicturebackend.manager.websocket.model.PictureEditResponseMessage;
import com.wyy.yunpicture.domain.user.entity.User;
import com.wyy.yunpicture.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    // 正在编辑图片的用户，存储图片id和用户id
    private final Map<Long, Long> editingPictureUsers = new ConcurrentHashMap<>();

    // 协同编辑图片的所有用户信息
    private final Map<Long, Set<WebSocketSession>> editingPictureSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 将图片id和session保存到集合中
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");
        editingPictureSessions.putIfAbsent(pictureId, new ConcurrentHashMap<>().newKeySet());
        editingPictureSessions.get(pictureId).add(session);
        // 给响应体赋值
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUserVO(userApplicationService.getUserVO(user));
        // 广播给同一张图片的所有用户
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }

    /**
     * 广播消息
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession 排除掉的用户
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,
                                    WebSocketSession excludeSession)
            throws IOException {
        // 取出session集合
        Set<WebSocketSession> webSocketSessions = editingPictureSessions.get(pictureId);
        if (ObjUtil.isNotEmpty(webSocketSessions)) {
            // 将响应体java对象转为JSON字符串
            String responseMessage = objectMapper.writeValueAsString(pictureEditResponseMessage);
            // 将JSON字符串转为对应的包装类型
            TextMessage responseTextMessage = new TextMessage(responseMessage);
            // 排除掉excludeSession，给其他session发消息
            for (WebSocketSession webSocketSession : webSocketSessions) {
                if (excludeSession != null && excludeSession.equals(webSocketSession)) {
                    continue;
                }
                if (webSocketSession.isOpen()) {
                    webSocketSession.sendMessage(responseTextMessage);
                }
            }
        }
    }

    /**
     * 广播给所有用户（包括自己）
     * @param pictureId
     * @param pictureEditResponseMessage
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage)
            throws IOException {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

    /**
     * 对前端发来的消息进行处理
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息转为java对象
        String requestMessageJson = message.getPayload();
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(requestMessageJson, PictureEditRequestMessage.class);
        String pictureEditRequestMessageType = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(pictureEditRequestMessageType);
        // 将session中的用户信息取出
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        pictureEditEventProducer.publishEvent(pictureEditRequestMessage, session, pictureId, user);
        /*// 根据消息类型，执行不同的处理方法
        switch (pictureEditMessageTypeEnum) {
            case ENTER_EDIT:
                handleEnterEditMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            case EDIT_ACTION:
                handleEditActionMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            case EXIT_EDIT:
                handleExitEditMessage(pictureEditRequestMessage, session, pictureId, user);
                break;
            default:
                PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
                pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ERROR.getValue());
                pictureEditResponseMessage.setMessage("消息类型错误");
                pictureEditResponseMessage.setUserVO(userApplicationService.getUserVO(user));
                String pictureEditResponseMessageJson = objectMapper.writeValueAsString(pictureEditResponseMessage);
                TextMessage textMessage = new TextMessage(pictureEditResponseMessageJson);
                session.sendMessage(textMessage);
        }*/

    }

    /**
     * 处理进入编辑消息
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                        Long pictureId, User user) throws IOException {
        // 没有用户正在编辑，才可进入编辑状态
        if (!editingPictureUsers.containsKey(pictureId)) {
            // 设置为编辑状态
            editingPictureUsers.put(pictureId, user.getId());
            // 广播消息
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("%s开始编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVO(userApplicationService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * 处理编辑动作消息
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                         Long pictureId, User user) throws IOException {
        // 确定当前用户是编辑者（当前图片id对应的是当前用户）
        Long userId = user.getId();
        Long editingUserId = editingPictureUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(userId)) {
            // 校验参数
            String editAction = pictureEditRequestMessage.getEditAction();
            PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
            if (pictureEditActionEnum == null) {
                log.error("无效的编辑动作");
                return;
            }
            // 设置响应消息值
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("%s执行%s", user.getUserName(), pictureEditActionEnum.getText());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setEditAction(pictureEditActionEnum.getValue());
            pictureEditResponseMessage.setUserVO(userApplicationService.getUserVO(user));
            // 广播（排除掉自己，否则会重复编辑）
            broadcastToPicture(pictureId, pictureEditResponseMessage, session);
        }


    }

    /**
     * 处理退出编辑消息
     * @param pictureEditRequestMessage
     * @param session
     * @param pictureId
     * @param user
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session,
                                       Long pictureId, User user) throws IOException {
        Long editingPictureUserId = editingPictureUsers.get(pictureId);
        // 查看editingPictureUsers，且是否是当前用户
        if (editingPictureUserId != null && editingPictureUserId.equals(user.getId())) {
            // 移除用户
            editingPictureUsers.remove(pictureId);
            // 广播
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("%s退出编辑图片", user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUserVO(userApplicationService.getUserVO(user));
            broadcastToPicture(pictureId, pictureEditResponseMessage);
        }
    }

    /**
     * Websocket 连接关闭‌（包括但不限于：用户主动关闭、关闭/刷新网页、网络中断、浏览器崩溃），移除当前用户的编辑状态、并且从集合中删‌除当前会话
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long pictureId = (Long) attributes.get("pictureId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态并广播
        handleExitEditMessage(null, session, pictureId, user);

        Set<WebSocketSession> webSocketSessions = editingPictureSessions.get(pictureId);
        if (ObjUtil.isNotEmpty(webSocketSessions)) {
            // 将当前用户的session从session集合中移除
            webSocketSessions.remove(session);
            if (ObjUtil.isEmpty(webSocketSessions)) {
                // 若session集合为空，则移除对应图片对应的session集合
                editingPictureSessions.remove(pictureId);
            }
        }
        // 广播
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s用户离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUserVO(userApplicationService.getUserVO(user));
        broadcastToPicture(pictureId, pictureEditResponseMessage);
    }
}
