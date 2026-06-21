package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Battle;
import com.example.demo.model.BattleStatus;
import com.example.demo.model.PlaylistSong;
import com.example.demo.repo.BattleRepository;
import com.example.demo.repo.PlaylistRepository;

@Service
public class BattleService {
    private final BattleRepository battleRepository;
    private final PlaylistRepository playlistRepository;

    @Autowired
    public BattleService(BattleRepository battleRepository,
        PlaylistRepository playlistRepository
    ) {
        this.playlistRepository = playlistRepository;
        this.battleRepository = battleRepository;
    }

    public Battle createBattle(Battle battle) {
        return battleRepository.save(battle);
    }

    public Battle getBattle(String roomCode) {
        return battleRepository.findByRoomCode(roomCode).orElseThrow(() -> new RuntimeException("No battle found"));
    }

    public Battle endBattle(String roomCode) {
        Battle battle = battleRepository.findByRoomCode(roomCode)
            .orElseThrow(() -> new RuntimeException("Battle not found for room: " + roomCode));

        battleRepository.delete(battle);

        return battle;
    }

   public Battle votePlayer(String roomCode, String playerId) {

    Battle battle = battleRepository.findByRoomCode(roomCode)
        .orElseThrow(() -> new RuntimeException("Battle not found"));

    if (playerId.equals(battle.getPlayer1Id())) {
        battle.setPlayer1Score(
            (battle.getPlayer1Score() == null ? 0 : battle.getPlayer1Score()) + 1
        );
    } 
    else if (playerId.equals(battle.getPlayer2Id())) {
        battle.setPlayer2Score(
            (battle.getPlayer2Score() == null ? 0 : battle.getPlayer2Score()) + 1
        );
    } 
    else {
        throw new RuntimeException("Invalid player");
    }

    return battleRepository.save(battle);
}

    public String setWinner(String roomCode, String winnerId) {
        return "";
    }

    public String getWinner(String roomCode) {
        Battle battle = battleRepository.findByRoomCode(roomCode)
            .orElseThrow(() -> new RuntimeException("Battle not found"));

        return battle.getWinnerId();
    }

}
