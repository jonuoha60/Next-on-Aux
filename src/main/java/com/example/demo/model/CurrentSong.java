package com.example.demo.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
@Table(name = "current_song")
public class CurrentSong {
    @Id
    @GeneratedValue
    private Long id;

    private String songName;
    private String artist;
    private String roomCode;
    private Long duration;
    private String songBanner;
    private String songLink;
    private Long playlistSongId;
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "added_by")
    private String addedBy;
}
