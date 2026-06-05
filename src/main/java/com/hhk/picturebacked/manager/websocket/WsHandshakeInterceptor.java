package com.hhk.picturebacked.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.hhk.picturebacked.auth.SpaceUserAuthManager;
import com.hhk.picturebacked.auth.model.SpaceUserPermissionConstant;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.entity.Space;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.model.enums.SpaceTypeEnum;
import com.hhk.picturebacked.service.PictureService;
import com.hhk.picturebacked.service.SpaceService;
import com.hhk.picturebacked.service.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.NonNull;
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
    public UserService userService;
    @Resource
    public PictureService pictureService;
    @Resource
    public SpaceService spaceService;
    @Resource
    public SpaceUserAuthManager spaceUserAuthManager;

    /**
     * websocket连接之前进行权限校验
     */
    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) throws Exception {
        if(request instanceof ServletServerHttpRequest){
//            前端发起请求 ➡️ HTTP 协议传输 ➡️ 后端接收请求
//            后端代码动作：接收、解析 ➡️ 用 ServletUtil
//            后端发起请求（去调别的接口） ➡️ HTTP 协议传输 ➡️ 第三方接收
//            后端代码动作：主动发送、调用 ➡️ 用 HttpUtil
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String pictureId = servletRequest.getParameter("pictureId");
            if(StrUtil.isBlank(pictureId)){
                log.error("缺少图片参数，拒绝握手");
                return false;
            }
            User loginUser = userService.getLoginUser(servletRequest);
            if(ObjUtil.isEmpty(loginUser)){
                log.error("用户未登录，拒绝握手");
                return false;
            }
            Picture picture = pictureService.getById(pictureId);
            if(picture==null){
                log.error("图片不存在");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            // spaceId 为空或 0 表示公共图库，不支持协同编辑
            if (spaceId != null && spaceId > 0) {
                space = spaceService.getById(spaceId);
                if (space == null) {
                    log.error("空间不存在，拒绝握手");
                    return false;
                }
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.info("不是团队空间，拒绝握手");
                    return false;
                }
            } else {
                log.error("公共图库图片不支持协同编辑，拒绝握手");
                return false;
            }
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if(!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)){
                log.error("没有图片编辑权限,拒绝握手");
                return false;
            }
            //设置attributes
            attributes.put("user",loginUser);
            attributes.put("userId",loginUser.getId());
            attributes.put("pictureId",pictureId);
            return true;

        }

        return false;
    }
    /**
     * websocket连接后进行权限校验
     */
    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, Exception exception) {

    }
}
