package com.df.framework.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/dog/websocket/{uid}")
public class WebSocketEndpoint {

    /**
     * 用于存所有的连接服务的客户端
     */
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap();
    /**
     * 用于存所有的连接服务的客户端，判断是否是关闭浏览器，进而退出用户，key是Session
     */
    public static ConcurrentHashMap<String, Date> sessionLiveMap = new ConcurrentHashMap();


    /**
     * 连接建立成功调用的方法
     *
     * @param uid
     * @param session
     */
    @OnOpen
    public void onOpen(@PathParam("uid") String uid, Session session) {
        if (uid != null) {
            sessionMap.put(uid, session);
            sessionLiveMap.put(uid, new Date());
            log.info("【webSocket 建立连接】 uid：{}", uid);
            /*Map<String, Object> data = new HashMap<String, Object>();
            data.put("url", "/topic/welcome");
            data.put("data", "welcome client for " + uid);
            String msgToSend = JSON.toJSONString(data);
            try {
                session.getBasicRemote().sendText(msgToSend);
            } catch (IOException e) {
                log.error("{}", e);
            }*/
        } else {
            log.error("前端传递来的uid为空");
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param uid
     * @param message
     * @param session
     * @throws IOException
     */
    @OnMessage
    public void onMessage(@PathParam("uid") String uid, String message, Session session) {
        //只要前端发消息就更新容器中用户对应的用户请求时间
        log.info("【webSocket 建立连接】 uid：{} msg：{}", uid, message);
        sessionMap.put(uid, session);
        sessionLiveMap.put(uid, new Date());
    }

    /**
     * 发生错误时调用
     *
     * @param uid
     * @param e
     */
    @OnError
    public void onError(@PathParam("uid") String uid, Throwable e) {
        sessionMap.remove(uid);
        sessionLiveMap.remove(uid);
        //log.error("【webSocket 连接错误】 uid：{}", uid);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param uid
     * @param session
     * @param reason
     */
    @OnClose
    public void onClose(@PathParam("uid") String uid, Session session, CloseReason reason) {
        sessionMap.remove(uid);
        sessionLiveMap.remove(uid);
        //log.info("【webSocket 连接关闭】 uid：{}", uid);
    }


    /**
     * 给所有用户发送ws消息
     *
     * @param url
     * @param data
     */
    public synchronized void sendMsg(String url, Object data) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("url", url);
        msg.put("data", data);

        String jsonMsg = JSON.toJSONString(msg, SerializerFeature.WriteDateUseDateFormat);
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            String key = entry.getKey();
            Session session = entry.getValue();
            try {
                session.getAsyncRemote().sendText(jsonMsg);
                //log.info("【webSocket消息发送】：uid：{}，Message：{}", key, jsonMsg);
            } catch (Exception e) {
                //log.error("给所有用户发送ws消息异常：{}", e.getMessage());
            }
        }
    }

    /**
     * 给指定用户发送ws消息
     *
     * @param url
     * @param data
     * @param userName
     */
    public synchronized boolean sendMsgToUser(String url, Object data, String userName) {
        Map<String, Object> msg = new HashMap<String, Object>();
        msg.put("url", url);
        msg.put("data", data);
        boolean send = false;
        String jsonMsg = JSON.toJSONString(msg);
        for (Map.Entry<String, Session> entry : sessionMap.entrySet()) {
            try {
                if (StringUtils.isNotBlank(userName) && entry.getKey().startsWith(userName)) {
                    Session session = entry.getValue();
                    session.getAsyncRemote().sendText(jsonMsg);
                    send = true;
                }
                if (StringUtils.isBlank(userName)) {
                    Session session = entry.getValue();
                    session.getAsyncRemote().sendText(jsonMsg);
                    send = true;
                }
            } catch (Exception e) {
                log.error("给指定用户发送ws消息异常：{}", e.getMessage());
            }
        }
        return send;
    }
}
