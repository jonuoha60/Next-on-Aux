package com.example.demo.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameMessage {
    private String opponent;
    private String opponentId;
    private Integer songCount;
    private String sender;
    private String category;
    private String roomCode;
    private String winnerId;
    private String player1Id;
    private String player2Id;
    private String player1Name;
    private String player2Name;
    private Integer player1Score;
    private Integer player2Score;
    private Boolean liveVoting;
    private MessageType type;

}