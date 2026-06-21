package com.example.demo.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.Battle;

@Repository
public interface BattleRepository extends JpaRepository<Battle, Long>{
    Optional<Battle> findByRoomCode(String roomCode);
}
