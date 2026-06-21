package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("""
        SELECT DISTINCT r
        FROM Room r
        LEFT JOIN FETCH r.users
        WHERE r.roomCode = :roomCode
    """)
    Optional<Room> findByRoomCodeWithUsers(@Param("roomCode") String roomCode);

    Optional<Room> findByRoomCode(String roomCode);

    List<Room> findByPrivateRoomFalse();

    @Transactional
    @Modifying
    void deleteByRoomCode(String roomCode);
}