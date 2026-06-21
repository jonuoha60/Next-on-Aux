package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "battle")
public class Battle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    
    @Column(name = "roomCode")
    private String roomCode;

    private String player1Id;
    private String player2Id;

    private int songCount;

    @Enumerated(EnumType.STRING)
    private BattleStatus gameStatus; // WAITING, ACTIVE, FINISHED

    @OneToMany(mappedBy = "battle",
                cascade = CascadeType.ALL,
                orphanRemoval = true)
    @JsonIgnore 
    private List<PlaylistSong> songs = new ArrayList<>();

    @Column(name = "player1_score")
    private Integer player1Score = 0;

    @Column(name = "player2_score")
    private Integer player2Score = 0;

    private String winnerId;

    private Boolean liveVoting;

    @Column(name = "category", columnDefinition= "TEXT")
    private String category;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;



}
