package com.wyy.yunpicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.wyy.yunpicturebackend.manager.auth.SpaceUserAuthManager;
import com.wyy.yunpicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.wyy.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.wyy.yunpicturebackend.service.PictureService;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.UserService;
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
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            // 转为servletRequest对象
            ServletServerHttpRequest request1 = (ServletServerHttpRequest) request;
            HttpServletRequest servletRequest = request1.getServletRequest();
            // 获取参数（图片id和用户）
            String pictureIdStr = servletRequest.getParameter("pictureId");
            Long pictureId = Long.valueOf(pictureIdStr);
            User currentUser = userService.getCurrentUser(servletRequest);
            // 基本校验和权限校验
            if (ObjUtil.isEmpty(pictureId)) {
                log.error("缺少图片id，拒绝握手");
                return false;
            }
            if (ObjUtil.isEmpty(currentUser)) {
                log.error("用户未登录，拒绝握手");
                return false;
            }
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝握手");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                // 校验空间类型
                if (SpaceTypeEnum.TEAM.getValue() != space.getSpaceType()) {
                    log.error("不是团队空间，拒绝握手");
                    return false;
                }
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, currentUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有图片编辑权限，拒绝握手");
                return false;
            }
            // 将数据放在attributes中
            attributes.put("user", currentUser);
            attributes.put("pictureId", pictureId);
            attributes.put("userId", currentUser.getId());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
