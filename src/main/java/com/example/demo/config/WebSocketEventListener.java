package com.example.demo.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import com.example.demo.model.ChatMessage;
import com.example.demo.model.MessageType;
import com.example.demo.model.Room;
import com.example.demo.model.RoomDTO;
import com.example.demo.model.User;
import com.example.demo.service.RoomService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messageTemplate;
    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private Map<String, Integer> topicSubscriberCount;
    private RoomService roomService;
    private Map<String, String> usersMap = new HashMap<>();

    // private String userId;
    

    @Autowired
    private SimpUserRegistry userRegistry;
    // private Random rand;

    @Autowired
    public WebSocketEventListener(
            @Qualifier("topicSubCount") Map<String, Integer> topicSubCount, 
            SimpMessageSendingOperations messageTemplate,
            RoomService roomService
        ) {
        this.roomService = roomService;
        this.topicSubscriberCount = topicSubCount;
        this.messageTemplate = messageTemplate;
    }

 
  // Disconnect handler
   @EventListener
    public void handleWebSocketEventListener(SessionDisconnectEvent event) {

        StompHeaderAccessor headerAccessor =
                StompHeaderAccessor.wrap(event.getMessage());

        String username = (String) headerAccessor.getSessionAttributes().get("username");
        String roomCode = (String) headerAccessor.getSessionAttributes().get("roomCode");

        if (roomCode == null || username == null) return;

        Room room = roomService.findByRoomCode(roomCode);

        User leavingUser = room.getUsers()
                .stream()
                .filter(u -> username.equals(u.getUsername()))
                .findFirst()
                .orElse(null);

        if (leavingUser != null) {
            room.getUsers().remove(leavingUser);
            roomService.save(room);
        }

        ChatMessage chatMessage = ChatMessage.builder()
                .type(MessageType.LEAVE)
                .sender(username)
                .userId(leavingUser != null ? leavingUser.getId().toString() : null)
                .build();

        messageTemplate.convertAndSend("/topic/room." + roomCode, chatMessage);
    }

    // Connected handler
    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        log.info("Connect", headerAccessor);


        if(destination != null && destination.startsWith("/topic/room.")) {
            String roomCode = destination.substring("/topic/room.".length());
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            
            headerAccessor.getSessionAttributes().put("roomCode", roomCode);
            Room myRoom = roomService.findByRoomCode(roomCode);


        RoomDTO roomDTO = RoomDTO.builder()
                .id(myRoom.getId())
                .roomCode(myRoom.getRoomCode())
                .roomName(myRoom.getRoomName())
                .host(myRoom.getHost())
                .users(myRoom.getUsers())
                .build();

        var chatMessage = ChatMessage.builder()
                .type(MessageType.JOIN)
                .sender(username)
                .room(roomDTO)
                .build();
                messageTemplate.convertAndSend("/topic/room." + roomCode, chatMessage);
                System.out.println("THE USERS IN MY ROOM" + myRoom.getUsers().size());
                System.out.println("Notified room " + roomCode + " that " + username + " has joined " + " with userid of ");
                System.out.println("Room Data: " + myRoom.getHost());
        }
    }

   
}
