package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "playlist_song")
public class PlaylistSong {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;
    
    @Column(name = "roomCode")
    private String roomCode;

    @Column(name = "artist")
    private String artist;

    @Column(name = "songName")
    private String songName;
  
    @Column(name = "duration")
    private Long duration;

    @Column(name = "songBanner", columnDefinition= "TEXT")
    private String songBanner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id")
    private Battle battle;

    private Integer queuePosition; 


    @Column(name = "songLink", columnDefinition= "TEXT")
    private String songLink;




    @Column(name = "added_by")
    private String addedBy;


}
