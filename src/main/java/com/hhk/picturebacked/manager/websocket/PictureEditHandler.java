package com.hhk.picturebacked.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.hhk.picturebacked.manager.websocket.model.PictureEditActionEnum;
import com.hhk.picturebacked.manager.websocket.model.PictureEditMessageTypeEnum;
import com.hhk.picturebacked.manager.websocket.model.PictureEditRequestMessage;
import com.hhk.picturebacked.manager.websocket.model.PictureEditResponseMessage;
import com.hhk.picturebacked.model.dto.picture.PictureEditRequest;
import com.hhk.picturebacked.model.entity.User;
import com.hhk.picturebacked.service.UserService;
import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.wrapper.ObjectWrapper;
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

/**
 * websocket处理类
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    @Resource
    public UserService userService;

    @Resource
    private PictureEditEventProducer pictureEditEventProducer;

    //每张图片的编辑状态，key表示pictureId value 表示当前正在编辑的用户Id
    private final Map<Long,Long> pictureEditUsers=new ConcurrentHashMap<>();
    //保存所有链接的会话，key：pictureId value 表示：用户会话集合 （由于有多个Websocket集合 要用ConcurrentMap保证线程安全）
    private final Map<Long, Set<WebSocketSession>> pictureSessions=new ConcurrentHashMap<>();


    // 1. 【连接建立成功】客户端连上 ws 后立即执行
    // 用途：把 session 加入在线列表、获取用户信息、初始化
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        User user = (User) session.getAttributes().get("user");

        // 先拿 String，再转 Long —— 这才是对的
        String pictureIdStr = (String) session.getAttributes().get("pictureId");
        Long pictureId = Long.valueOf(pictureIdStr);
        //把 session 加入在线集合
        pictureSessions.putIfAbsent(pictureId,ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
        //广播
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message=String.format("%s 加入编辑",user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVo(user));
        brocadcastToPicture(pictureId,pictureEditResponseMessage);

    }

    // 2. 【收到文本消息】客户端发消息时执行
    // 用途：解析消息、处理业务、广播消息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {


        User user = (User) session.getAttributes().get("user");

        // 先拿 String，再转 Long —— 这才是对的
        String pictureIdStr = (String) session.getAttributes().get("pictureId");
        Long pictureId = Long.valueOf(pictureIdStr);
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(message.getPayload(), PictureEditRequestMessage.class);


        pictureEditEventProducer.publishEvent(pictureEditRequestMessage ,session,user,pictureId);




    }

    /**
     * 执行编辑操作
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     * @throws IOException
     */
    public void handleEnterActionMassage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        //确认当前编辑用户是不是本人
        Long editingUserId = pictureEditUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum editActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if(editActionEnum==null){
            return;
        }
        if(editingUserId!=null&&user.getId()==editingUserId){
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setEditAction(editActionEnum.getValue());
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            pictureEditResponseMessage.setUser(userService.getUserVo(user));
            String message=String.format("%s执行%s",user.getUserName(),editActionEnum.getValue());
            pictureEditResponseMessage.setMessage(message);
            brocadcastToPicture(pictureId,pictureEditResponseMessage,session);

        }


    }

    /**
     * 退出编辑状态
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMassage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        Long editingUserId = pictureEditUsers.get(pictureId);
        if(editingUserId!=null&&editingUserId.equals(user.getId())){
            pictureEditUsers.remove(pictureId);
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            String message=String.format("%s 退出编辑",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setUser(userService.getUserVo(user));
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            brocadcastToPicture(pictureId,pictureEditResponseMessage);
        }

    }

    /**
     * 进入编辑状态
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMassage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) throws IOException {

        //判断有没有人进入编辑状态，才能进行编辑
        if(!pictureEditUsers.containsKey(pictureId)){
            pictureEditUsers.put(pictureId, user.getId());
            PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
            pictureEditResponseMessage.setUser(userService.getUserVo(user));
            String message=String.format("%s 开始编辑图片",user.getUserName());
            pictureEditResponseMessage.setMessage(message);
            pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            brocadcastToPicture(pictureId,pictureEditResponseMessage);
        }

    }

    // 3. 【连接关闭】客户端断开连接时执行
    // 用途：从在线列表移除 session、清理资源
    @Override

    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {

        User user = (User) session.getAttributes().get("user");

        // 先拿 String，再转 Long —— 这才是对的
        String pictureIdStr = (String) session.getAttributes().get("pictureId");
        Long pictureId = Long.valueOf(pictureIdStr);
        // 移除当前用户的编辑状态
        handleExitEditMassage(null, session, user, pictureId);

        // 删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }

        // 响应
        PictureEditResponseMessage pictureEditResponseMessage = new PictureEditResponseMessage();
        pictureEditResponseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("%s离开编辑", user.getUserName());
        pictureEditResponseMessage.setMessage(message);
        pictureEditResponseMessage.setUser(userService.getUserVo(user));
        brocadcastToPicture(pictureId, pictureEditResponseMessage);
    }


    // 4. 【传输异常】发生错误时执行
    // 用途：打印日志、处理异常、关闭连接
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }


    /**
     * 广播响应消息
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     */
    private void brocadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage,WebSocketSession excludeSession) throws IOException {

        //取到pictureId所属的用户会话集合
        Set<WebSocketSession> webSocketSessions = pictureSessions.get(pictureId);
        if(CollUtil.isNotEmpty(webSocketSessions)){

            //解决精度缺失问题
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);

            //序列化为json字符串
            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            //给每个session发消息
            for (WebSocketSession session:webSocketSessions){
                if(excludeSession!=null&&excludeSession.equals(session)){
                    continue;
                }
                if (session.isOpen()){

                    session.sendMessage(textMessage);
                }
            }
        }


    }

    /**
     * 广播响应消息(不需要排除正在编辑的用户)
     * @param pictureId
     * @param pictureEditResponseMessage
     * @throws IOException
     */
    private void brocadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws IOException {
        brocadcastToPicture( pictureId,  pictureEditResponseMessage,null);
    }





}
