package com.example.demo.model;
import java.util.List;

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
public class RoomDTO {

    private Long id;
    private String roomCode;
    private String roomName;
    private List<User> users;
    private User host;
}