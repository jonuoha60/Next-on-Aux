package com.example.demo.model;

import java.time.LocalDateTime;
import java.util.List;

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
public class SongMessage {
    private String sender;
    private String songName;
    private String playlistSongId;
    private String artist;
    private String songBanner;
    private String songLink;
    private String duration;
    private String addedBy;
    private Boolean battleActive;
    private LocalDateTime startedAt;
    private MessageType type;
    private List<PlaylistSong> songs;



}
