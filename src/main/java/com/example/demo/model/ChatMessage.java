package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

    private String content;
    private String userId;
    private String sender;
    private RoomDTO room;
    private MessageType type;
}
