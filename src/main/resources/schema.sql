-- Active: 1781639712683@@127.0.0.1@3306@user
CREATE TABLE myuser (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    fireBaseUid VARCHAR(100),
    photoUrl VARCHAR(100),
    authProvider VARCHAR(100),
    password VARCHAR(100)
);
CREATE TABLE room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    host_id BIGINT NOT NULL,

    roomCode VARCHAR(100) NOT NULL,
    roomName VARCHAR(100) NOT NULL,

    access_token TEXT,
    refresh_token TEXT,

    private_room BOOLEAN,
    maxUser INT NOT NULL,

    CONSTRAINT fk_room_host
        FOREIGN KEY (host_id)
        REFERENCES myuser(id)
);

CREATE TABLE room_users (
    room_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,

    PRIMARY KEY (room_id, user_id),

    CONSTRAINT fk_room_users_room
        FOREIGN KEY (room_id)
        REFERENCES room(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_room_users_user
        FOREIGN KEY (user_id)
        REFERENCES myuser(id)
        ON DELETE CASCADE
);

CREATE TABLE current_song (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    room_code VARCHAR(255) NOT NULL,
    playlist_song_id BIGINT,
    song_name VARCHAR(255),
    artist VARCHAR(255),
    addedBy VARCHAR(100),
    duration BIGINT,
    song_banner TEXT,
    song_link TEXT,

    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE playlist_song (
    id INT AUTO_INCREMENT PRIMARY KEY,
    queuePosition INT NOT NULL,
    songName VARCHAR(100) NOT NULL,
    artist VARCHAR(100) NOT NULL,
    songBanner VARCHAR(100) NOT NULL,
    addedBy VARCHAR(100),
    songLink VARCHAR(500) NOT NULL,
    duration VARCHAR(500) NOT NULL,
    battle_id BIGINT,
    roomCode VARCHAR(500) NOT NULL,
    CONSTRAINT fk_battle
            FOREIGN KEY (battle_id)
            REFERENCES battle(id)
);

CREATE TABLE battle (
    id INT AUTO_INCREMENT PRIMARY KEY,
    roomCode VARCHAR(500) NOT NULL,
    songCount BIGINT,
    winnerId VARCHAR(100),
    liveVoting BOOLEAN DEFAULT FALSE,
    player1Score BIGINT DEFAULT 0,
    player2Score BIGINT DEFAULT 0,
    player1Id VARCHAR(100) NOT NULL,
    player2Id VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    gameStatus VARCHAR(100) NOT NULL,
    startedAt DATE,
    endedAt DATE

)

CREATE TABLE refresh_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES myuser(id)
        ON DELETE CASCADE
);
