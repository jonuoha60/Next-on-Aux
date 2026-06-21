package com.example.demo.model;


import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "room")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;



    @Column(name = "roomCode")
    private String roomCode;
  
    @Column(name = "roomName")
    private String roomName;

    @Column(name = "access_token", columnDefinition= "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition= "TEXT")
    private String refreshToken;

    @Column(name = "maxUser")
    private Integer maxUser;

    @Column(name = "private_room")
    private Boolean privateRoom;

    @ManyToOne
    @JoinColumn(name = "host_id")
    private User host;

    @ManyToMany
    @JoinTable(
        name = "room_users",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users = new ArrayList<>();

}
