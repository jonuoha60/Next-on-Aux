package com.example.demo.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
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
@Table(name = "myuser")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String fireBaseUid;
    private String photoUrl;
    private String authProvider;
    
    @JsonIgnore
    @OneToMany(mappedBy = "user")
    private List<RefreshToken> tokens;

    @JsonIgnore
    @OneToMany(mappedBy = "host")
    private List<Room> hostedRooms = new ArrayList<>();

    @JsonIgnore
    @ManyToMany(mappedBy = "users")
    private List<Room> joinedRooms = new ArrayList<>();

}