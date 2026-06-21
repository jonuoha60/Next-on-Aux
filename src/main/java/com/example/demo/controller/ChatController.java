package com.example.demo.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.GameMessage;
import com.example.demo.model.SongMessage;

@Controller
public class ChatController {
    
    @MessageMapping("/chat/{roomCode}/send")
    @SendTo("/topic/room.{roomCode}")
    public ChatMessage sendMessage(
        @DestinationVariable String roomCode,
        @Payload ChatMessage chatMessage) {
            return chatMessage;
    }

    @MessageMapping("/chat/{roomCode}/game")
    @SendTo("/topic/room.{roomCode}")
    public GameMessage sendGame(
        @DestinationVariable String roomCode,
        @Payload GameMessage gameMessage) {
            return gameMessage;
    }
    
    @MessageMapping("/chat/{roomCode}/music")
    @SendTo("/topic/room.{roomCode}")
    public SongMessage sendSong(
        @DestinationVariable String roomCode,
        @Payload SongMessage songMessage) {
            return songMessage;
    }
    
    @MessageMapping("/chat/{roomCode}/queue")
    @SendTo("/topic/room.{roomCode}")
    public SongMessage sendQueue(
        @DestinationVariable String roomCode,
        @Payload SongMessage songMessage) {
            return songMessage;
    }

    // @MessageMapping("/chat/{roomCode}/join")
    // @SendTo("/topic/room.{roomCode}")
    // public ChatMessage addUser(
    //     @DestinationVariable String roomCode,
    //     @Payload ChatMessage chatMessage,
    //     SimpMessageHeaderAccessor headerAccessor
    // ) {
    //     headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
    //     // chatMessage.setContent("Room [" + roomCode + "]: " + chatMessage.getContent());
    //     return chatMessage;
    // }
}
