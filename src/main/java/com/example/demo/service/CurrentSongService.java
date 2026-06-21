package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.CurrentSong;
import com.example.demo.repo.CurrentRepository;

@Service
public class CurrentSongService {
    private final CurrentRepository currentRepository;

    @Autowired
    public CurrentSongService(CurrentRepository currentRepository) {
        this.currentRepository = currentRepository;
    }

    public CurrentSong getCurrentlyPlaying(String roomCode) {
    return currentRepository
            .findByRoomCode(roomCode)
            .orElse(null);
    }

    public CurrentSong setCurrentlyPlaying(String roomCode, CurrentSong song) {

        if (song == null) {
            throw new RuntimeException("Song cannot be null");
        }

        CurrentSong current = currentRepository
                .findByRoomCode(roomCode)
                .orElseGet(CurrentSong::new);

        current.setRoomCode(roomCode);

        current.setSongName(song.getSongName());
        current.setArtist(song.getArtist());
        current.setDuration(song.getDuration());
        current.setSongBanner(song.getSongBanner());
        current.setSongLink(song.getSongLink());
        current.setStartedAt(song.getStartedAt());
        current.setAddedBy(song.getAddedBy());


        // if (song.getPlaylistSongId() == null) {
        //     throw new RuntimeException("playlistSongId cannot be null");
        // }

        current.setPlaylistSongId(song.getPlaylistSongId());

        return currentRepository.save(current);
    }

    
}
