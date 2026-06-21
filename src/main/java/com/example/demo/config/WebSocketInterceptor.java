package com.example.demo.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketInterceptor implements ChannelInterceptor {

    //To get username before it subscribes
   @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String username = accessor.getFirstNativeHeader("username");
            String roomCode = accessor.getFirstNativeHeader("roomCode");

            accessor.getSessionAttributes().put("username", username);
            accessor.getSessionAttributes().put("roomCode", roomCode);
        }
        return message;
    }
}