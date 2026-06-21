package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.model.PlaylistSong;
import com.example.demo.model.Room;

@Repository
public interface PlaylistRepository extends JpaRepository<PlaylistSong, Long> {
    List<PlaylistSong> findByRoomCode(String roomCode);
    List<PlaylistSong> findByRoomCodeOrderByQueuePositionAsc(String roomCode);

    Optional<PlaylistSong> findFirstByRoomCodeOrderByQueuePositionAsc(String roomCode);

    @Query("""
        SELECT COALESCE(MAX(p.queuePosition), 0)
        FROM PlaylistSong p
        WHERE p.roomCode = :roomCode
    """)
    Integer findMaxQueuePosition(String roomCode);
    Long countByRoomCodeAndAddedBy(
        String roomCode,
        String addedBy
    );
}
