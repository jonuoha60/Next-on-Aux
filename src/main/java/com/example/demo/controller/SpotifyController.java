package com.example.demo.controller;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.Battle;
import com.example.demo.model.CurrentSong;
import com.example.demo.model.PlaylistSong;
import com.example.demo.model.Room;
import com.example.demo.service.BattleService;
import com.example.demo.service.CurrentSongService;
import com.example.demo.service.PlaylistService;
import com.example.demo.service.RoomService;
import com.example.demo.service.SpotifyService;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletResponse;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;

@RestController
@RequestMapping("/spotify")
public class SpotifyController {
    private final RoomService roomService;
    private final SpotifyService spotifyService;
    private final PlaylistService playlistService;
    private final CurrentSongService currentSongService;
    private final BattleService battleService;

    @Autowired
    public SpotifyController(
        RoomService roomService, 
        CurrentSongService currentSongService,
        SpotifyService spotifyService, 
        PlaylistService playListService,
        BattleService battleService
    ) {
        this.currentSongService = currentSongService;
        this.battleService = battleService;
        this.playlistService = playListService;
        this.roomService = roomService;
        this.spotifyService = spotifyService;
    }

    @GetMapping("/login")
    public void login(
        @RequestParam String roomCode,
        @RequestParam String roomName,
        HttpServletResponse response) throws IOException {
        List<String> scopes = List.of(
             "user-read-private",
                "user-read-playback-state",
                "user-modify-playback-state",
                "streaming",
                "user-read-email"
        );
        Dotenv dotenv = Dotenv.load();

        String clientId = dotenv.get("CLIENT_ID");
        String redirectUri = dotenv.get("REDIRECT_URL");

        String scopeParam = String.join(" ", scopes);

        String state = roomCode + "|" + roomName;


        String authUrl =
                "https://accounts.spotify.com/authorize" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&scope=" + URLEncoder.encode(scopeParam, StandardCharsets.UTF_8) +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        response.sendRedirect(authUrl);


    }

    @GetMapping("/callback")
    public String callback(
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "error", required = false) String error,
        @RequestParam(value = "state", required = false) String state,
        HttpServletResponse response
    ) throws Exception {

    if (error != null) {
        return "Spotify auth error: " + error;
    }

     if (code == null) {
        return "Missing Spotify authorization code. Do not open this URL manually.";
    }

    Dotenv dotenv = Dotenv.load();

    String[] parts = state.split("\\|");

    String roomCode = parts[0];
    String roomName = parts[1];

    String clientId = dotenv.get("CLIENT_ID");
    String clientSecret = dotenv.get("CLIENT_SECRET");
    String redirectUri = dotenv.get("REDIRECT_URL");

    SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRedirectUri(java.net.URI.create(redirectUri))
            .build();

    AuthorizationCodeRequest request = spotifyApi.authorizationCode(code).build();
    AuthorizationCodeCredentials credentials = request.execute();

    String accessToken = credentials.getAccessToken();
    String refreshToken = credentials.getRefreshToken();
    Integer expiresIn = credentials.getExpiresIn();

    spotifyApi.setAccessToken(accessToken);
    spotifyApi.setRefreshToken(refreshToken);
    
    roomService.addToken(roomCode, accessToken, refreshToken);

    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    Runnable refreshTask = () -> {
        try{
        AuthorizationCodeRefreshRequest refreshRequest = spotifyApi.authorizationCodeRefresh().build();

        AuthorizationCodeCredentials refreshedCredentials = refreshRequest.execute();

        String newAccessToken = refreshedCredentials.getAccessToken();

        spotifyApi.setAccessToken(newAccessToken);

        roomService.addToken(roomCode, newAccessToken, refreshToken);
        } catch(Exception e) {
            e.printStackTrace();
        }


    };

        scheduler.scheduleAtFixedRate(
                refreshTask,
                expiresIn - 300,
                expiresIn - 300,
                TimeUnit.SECONDS
        );

    System.out.println("ACCESS TOKEN: " + accessToken);

    response.sendRedirect("http://localhost:8080/room/" + roomCode + "/" + roomName);
    return "OK";

    }

    @GetMapping("/track")
    public ResponseEntity<String> getTrack(
            @RequestParam String param,
            @RequestParam String roomCode) {

        try {
            String accessToken = roomService.getAccessToken(roomCode);

            System.out.println("Token: " + accessToken);

            System.out.println("Room: " + roomCode);
            System.out.println("Token: " + accessToken);

            String trackData = spotifyService.searchTracks(param, accessToken);

            return ResponseEntity.ok(trackData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping("/addToPlaylist")
    public ResponseEntity<PlaylistSong> addSong(
        @RequestBody PlaylistSong playlistSong
    ) {

        System.out.println("Added song: " + playlistSong);

        
        return ResponseEntity.ok(playlistService.addPlaylist(playlistSong));
    }

    @GetMapping("/getRoom")
    public ResponseEntity<List<Room>> getPublicRoom() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }
    

    @GetMapping("/getPlaylist")
    public ResponseEntity<List<PlaylistSong>> getPlaylist(@RequestParam String roomCode) {


        List<PlaylistSong> playlist = playlistService.getQueue(roomCode);

        System.out.println("PLAYLIST SIZE: " + playlist.size());

        return ResponseEntity.ok(playlist);
    }

    @GetMapping("/getCurrentlyPlaying")
    public ResponseEntity<CurrentSong> getCurrentlyPlaying(@RequestParam String roomCode) {

        CurrentSong song = currentSongService.getCurrentlyPlaying(roomCode);

        if (song == null) {
            return ResponseEntity.ok(null); 
        }

        return ResponseEntity.ok(song);
    }

    @PostMapping("/updateCurrentlyPlaying")
    public ResponseEntity<CurrentSong> setCurrentlyPlaying(
            @RequestBody CurrentSong song,
            @RequestParam String roomCode
    ) {
        CurrentSong current = currentSongService.setCurrentlyPlaying(roomCode, song);
        return ResponseEntity.ok(current);
    }
    
        
    @PostMapping("/createBattle")
    public ResponseEntity<Battle> createBattle(
    @RequestBody Battle battle
    ) {
        Battle battle1 = battleService.createBattle(battle);

        return ResponseEntity.ok(battle1);
    }

    @PostMapping("/endBattle")
    public ResponseEntity<Battle> endRoomBattle(@RequestParam String roomCode) {
        Battle battle = battleService.endBattle(roomCode);
        return ResponseEntity.ok(battle);
    }

    @GetMapping("/getBattle")
    public ResponseEntity<Battle> getBattle(
            @RequestParam String roomCode
    ) {
        Battle battle1 = battleService.getBattle(roomCode);
        return ResponseEntity.ok(battle1);
    }


    @PostMapping("/playNext")
    public ResponseEntity<PlaylistSong> playNextSong(
            @RequestParam String roomCode,
            @RequestParam Long songId,
            @RequestParam(required = false) String startedAt 
    ) {
        PlaylistSong playlist = playlistService.playNext(roomCode, songId, startedAt);
        return ResponseEntity.ok(playlist);
    }

    @PostMapping("/playPrev")
    public ResponseEntity<PlaylistSong> playPrevSong(
            @RequestParam String roomCode,
            @RequestParam Long songId,
            @RequestParam(required = false) String startedAt 
    ) {
        PlaylistSong playlist = playlistService.playPrev(roomCode, songId, startedAt);
        return ResponseEntity.ok(playlist);
    }

    
    @PostMapping("/addSongForUser")
    public ResponseEntity<PlaylistSong> addSongUser(
        @RequestBody PlaylistSong song,
        @RequestParam String userId,
        @RequestParam String roomCode
    ) {
        PlaylistSong songs = playlistService.addSongForBattle(roomCode, userId, song);
        
        return ResponseEntity.ok(songs);
    }

    @GetMapping("/getWinner")
    public String getWinner(@RequestParam String roomCode) {
        return battleService.getWinner(roomCode);
    }
    
    @PostMapping("/votePlayer")
    public ResponseEntity<Battle> votePlayer(
            @RequestParam String roomCode,
            @RequestBody Map<String, String> body
    ) {
        String playerId = body.get("playerId");

        Battle updatedBattle = battleService.votePlayer(roomCode, playerId);

        return ResponseEntity.ok(updatedBattle);
    }

    @PostMapping("/deleteSong")
    public ResponseEntity<List<PlaylistSong>> deleteSong(
            @RequestParam String songId,
            @RequestParam String roomCode) {

        playlistService.deleteSong(Long.parseLong(songId));
        List<PlaylistSong> updatedPlaylist = playlistService.getPlaylist(roomCode);

        return ResponseEntity.ok(updatedPlaylist);
    }
    
    @PostMapping("/deleteRoom")
    public ResponseEntity<String> deleteRoom(@RequestParam String hostId,
            @RequestParam String roomCode) {
        try {
            roomService.deleteRoom(roomCode);
            return ResponseEntity.ok("Room deleted");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete room: " + e.getMessage());
        }
    }
    

    
}