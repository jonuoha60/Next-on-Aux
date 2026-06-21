package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.Room;
import com.example.demo.model.User;
import com.example.demo.repo.RoomRepository;

@Service
public class RoomService {
    
    private final RoomRepository roomRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> getPublicRooms() {
        return roomRepository.findByPrivateRoomFalse();
    }

    public void save(Room room) {
        roomRepository.save(room);
    }

    public boolean updateRoom(String roomCode, User user) {

        return roomRepository.findByRoomCode(roomCode)
                .map(room -> {

                    if (room.getUsers().size() >= room.getMaxUser()) {
                        return false;
                    }

                    boolean alreadyJoined = room.getUsers()
                            .stream()
                            .anyMatch(u -> u.getId().equals(user.getId()));

                    if (alreadyJoined) {
                        return true;
                    }

                    room.getUsers().add(user);

                    roomRepository.save(room);

                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void deleteRoom(String roomCode) {
        roomRepository.deleteByRoomCode(roomCode);
    }

    public void addToken(String roomCode, String accessToken, String refreshToken) {
        roomRepository.findByRoomCode(roomCode).ifPresent(room -> {
            room.setAccessToken(accessToken);
            room.setRefreshToken(refreshToken);
            roomRepository.save(room);
        });
    }

    public String getAccessToken(String roomCode) {
    return roomRepository.findByRoomCode(roomCode)
                    .map(Room::getAccessToken)
                    .orElse(null);
    }
        
    

    // public void deleteByRoomCode(String roomCode) {
    //     roomRepository.deleteByRoomCode(roomCode)
    //     .orElseThrow(() -> new IllegalArgumentException("Room code wasnt found" + roomCode ));
    // }

    // public void deleteUser(String user) {
    //     roomRepository.deleteUser(user)
    //     .orElseThrow(() -> new IllegalArgumentException("User wasnt found" + user));
    // }

    // Find a single room by roomcode
    public Room findByRoomCode(String roomCode) {
        return roomRepository.findByRoomCodeWithUsers(roomCode)
        .orElseThrow(() -> new IllegalArgumentException("Room code wasnt found" + roomCode ));
    }
    

}
