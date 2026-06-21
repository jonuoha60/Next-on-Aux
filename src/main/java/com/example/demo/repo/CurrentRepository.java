package com.example.demo.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.CurrentSong;

public interface CurrentRepository extends JpaRepository<CurrentSong, Object>{
    Optional<CurrentSong> findByRoomCode(String roomCode);
}
