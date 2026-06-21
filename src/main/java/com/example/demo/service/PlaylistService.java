package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.Battle;
import com.example.demo.model.CurrentSong;
import com.example.demo.model.PlaylistSong;
import com.example.demo.repo.BattleRepository;
import com.example.demo.repo.CurrentRepository;
import com.example.demo.repo.PlaylistRepository;

@Service
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final CurrentRepository currentRepository;
    private final BattleRepository battleRepository;

    @Autowired
    public PlaylistService(
        PlaylistRepository playlistRepository,
        CurrentRepository currentRepository,
        BattleRepository battleRepository
    ) {
        this.battleRepository = battleRepository;
        this.currentRepository = currentRepository;
        this.playlistRepository = playlistRepository;
    }

      public PlaylistSong addPlaylist(PlaylistSong playlist) {

        Integer maxPosition = playlistRepository.findMaxQueuePosition(playlist.getRoomCode());

        Integer nextPosition = (maxPosition == null) ? 1 : maxPosition + 1;
           

        System.out.println("Next Position" + nextPosition);

        playlist.setQueuePosition(nextPosition);

        return playlistRepository.save(playlist);
    }

    public List<PlaylistSong> getQueue(String roomCode) {
        return playlistRepository.findByRoomCodeOrderByQueuePositionAsc(roomCode);
    }

   

public PlaylistSong playNext(String roomCode, Long currentSongId, String startedAt) {

    List<PlaylistSong> songs = playlistRepository.findByRoomCodeOrderByQueuePositionAsc(roomCode);

    if (songs.isEmpty()) return null;

    LocalDateTime songStartTime = (startedAt != null && !startedAt.isBlank())
            ? LocalDateTime.parse(startedAt, DateTimeFormatter.ISO_DATE_TIME)
            : LocalDateTime.now();

    PlaylistSong next = null;

    for (int i = 0; i < songs.size(); i++) {
        if (songs.get(i).getId().equals(currentSongId)) {
            int nextIndex = (i + 1) % songs.size();
            next = songs.get(nextIndex);
            break;
        }
    }

    if (next == null) next = songs.get(0);

    CurrentSong current = currentRepository
        .findByRoomCode(roomCode)
        .orElseGet(CurrentSong::new);

    current.setRoomCode(roomCode);
    current.setPlaylistSongId(next.getId());
    current.setSongName(next.getSongName());
    current.setArtist(next.getArtist());
    current.setDuration(next.getDuration());
    current.setSongBanner(next.getSongBanner());
    current.setSongLink(next.getSongLink());
    current.setAddedBy(next.getAddedBy());
    current.setStartedAt(songStartTime);

    currentRepository.save(current);
    return next;
}

public PlaylistSong playPrev(String roomCode, Long currentSongId, String startedAt) {

    List<PlaylistSong> songs = playlistRepository.findByRoomCodeOrderByQueuePositionAsc(roomCode);

    if (songs.isEmpty()) return null;

    LocalDateTime songStartTime = (startedAt != null && !startedAt.isBlank())
            ? LocalDateTime.parse(startedAt, DateTimeFormatter.ISO_DATE_TIME)
            : LocalDateTime.now();

    PlaylistSong prev = null;

    for (int i = 0; i < songs.size(); i++) {
        if (songs.get(i).getId().equals(currentSongId)) {
            int prevIndex = (i - 1) % songs.size();
            prev = songs.get(prevIndex);
            break;
        }
    }

    if (prev == null) prev = songs.get(songs.size() - 1); // ← fallback to last song

    CurrentSong current = currentRepository
        .findByRoomCode(roomCode)
        .orElseGet(CurrentSong::new);

    current.setRoomCode(roomCode);
    current.setPlaylistSongId(prev.getId());
    current.setSongName(prev.getSongName());
    current.setArtist(prev.getArtist());
    current.setDuration(prev.getDuration());
    current.setSongBanner(prev.getSongBanner());
    current.setSongLink(prev.getSongLink());
    current.setAddedBy(prev.getAddedBy());
    current.setStartedAt(songStartTime);

    currentRepository.save(current);
    return prev;
}


public PlaylistSong addSongForBattle(
        String roomCode,
        String userId,
        PlaylistSong song
) {
    Battle battle = battleRepository.findByRoomCode(roomCode)
        .orElseThrow(() -> new RuntimeException("Battle not found"));

    // Validate this user is actually in the battle
    if (!userId.equals(battle.getPlayer1Id()) && !userId.equals(battle.getPlayer2Id())) {
        throw new RuntimeException("You are not a participant in this battle");
    }

    int maxSongs = battle.getSongCount() / 2;

    Long currentSongs = playlistRepository.countByRoomCodeAndAddedBy(roomCode, userId);

    if (currentSongs >= maxSongs) {
        throw new RuntimeException("You have reached your song limit");
    }

    long player1Songs = playlistRepository.countByRoomCodeAndAddedBy(roomCode, battle.getPlayer1Id());
    long player2Songs = playlistRepository.countByRoomCodeAndAddedBy(roomCode, battle.getPlayer2Id());

    boolean isPlayer1 = userId.equals(battle.getPlayer1Id());


    if (isPlayer1 && player1Songs > player2Songs) {
        throw new RuntimeException("It's Player 2's turn to add a song");
    }
    if (!isPlayer1 && player2Songs >= player1Songs) {
        throw new RuntimeException("It's Player 1's turn to add a song");
    }

    Integer maxPosition = playlistRepository.findMaxQueuePosition(roomCode);
    Integer nextPosition = (maxPosition == null) ? 1 : maxPosition + 1;

    song.setQueuePosition(nextPosition);
    song.setRoomCode(roomCode);
    song.setAddedBy(userId);
    song.setBattle(battle);

    return playlistRepository.save(song);
}

public void deleteSong(Long songId) {
    playlistRepository.deleteById(songId);
}

public List<PlaylistSong> getPlaylist(String roomCode) {
    return playlistRepository.findByRoomCodeOrderByQueuePositionAsc(roomCode);
}


}
