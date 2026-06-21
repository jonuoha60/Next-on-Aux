const roomCode = window.__ROOM_CODE__;
const roomHost = window.__ROOM_HOST__;
const hostId   = String(window.__ROOM_HOSTID__);

const user = JSON.parse(sessionStorage.getItem("user"));
const username = user?.username;

let stompClient      = null;
let userList         = new Map();
let userId           = null;
let selectedOpponent   = null;
let selectedOpponentId = null;
let songLink         = null;
let currentSongId    = null;
let battleData = {
    id: null, roomCode: null, player1Id: null, player2Id: null,
    songCount: 0, liveVoting: false, gameStatus: null, songs: [],
    startedAt: null, endedAt: null, player1Score: null, player2Score: null, winnerId: null
};
let battleActive     = false;
let currentSong      = null;
let progressInterval = null;
let endSongTimeout   = null;  
let playlistState    = [];

const isHost = () => username === roomHost;

const messageBody    = document.getElementById("message-body");
const messageContent = document.getElementById("message-content");
const users          = document.getElementById("user-list");
const battleUsers    = document.getElementById("battle-users");
const queueList      = document.getElementById("queueList");


function connect() {
    const socket = new SockJS("http://localhost:8080/ws");
    stompClient = Stomp.over(socket);
    stompClient.connect({ username, roomCode}, onConnected, onError);
}

function onConnected() {
    stompClient.subscribe(`/topic/room.${roomCode}`, onMessageReceived);
    loadInitialData();
}

function onError(error) { console.log("WebSocket Error:", error); }

const loadInitialData = async () => {
    try {
        await getPlaylist();
        await getCurrentSong();   
        
    } catch (err) {
        console.error("loadInitialData error:", err);
    }
};



function onMessageReceived(payload) {
    const message = JSON.parse(payload.body);

    if (message.type === 'LEAVE') {
        const leavingUserId = String(message.userId);
        if (leavingUserId === hostId) {
            onLeave()
        }
        console.log("LEAVE: ", message)
        userList.delete(leavingUserId);
    }

    if (message.type === 'JOIN' && message.room) {
        // room.users is now a List<User> (id, username) coming straight off the entity,
        // not a JSON-stringified map anymore
        userList.clear();
        (message.room.users || []).forEach(u => {
            userList.set(String(u.id), u.username);
        });
        getUserId();
        getCurrentSong();
        getPlaylist();
        getBattle();
    }

    if (message.type === 'GAME_START') {
        showBattleStarted(message);
    }

    if (message.type === 'GAME_END') {
        resetBattleUI();
    }

    if (message.type === 'START_MUSIC') {
        updateNowPlaying(message);
    }

    if (message.type === 'QUEUE') {
        getPlaylist();
    }

    if (message.type === 'DELETE_MUSIC') {
        renderQueue(message.songs);
    }

    if (message.type === 'VOTE') {
        updateBattleVotes(message);
    }
    if (message.type === 'DELETE_ROOM') {
        alert("The host has ended the room.");
        window.location.href = '/';
    }

    users.innerHTML = "";
    const ul = document.createElement('ul');
    userList.forEach((value, key) => {
        const li = document.createElement('li');
        li.classList.add("users-list");
        li.textContent = roomHost === value ? `host ${value}` : value;
        ul.appendChild(li);
    });
    users.appendChild(ul);

    battleUsers.innerHTML = "";
    const battleUl = document.createElement("ul");
    userList.forEach((value, key) => {
        if (value === username) return;
        const li = document.createElement("li");
        li.classList.add("users-list");
        li.textContent = value;
        li.dataset.userId   = key;
        li.dataset.username = value;
        battleUl.appendChild(li);
    });
    battleUsers.appendChild(battleUl);

    const messageElement = document.createElement("div");
    messageElement.classList.add("message");

    if      (message.type === 'JOIN')        messageElement.innerHTML = `<p><strong>${message.sender}</strong> joined the room</p>`;
    else if (message.type === 'LEAVE')       messageElement.innerHTML = `<p><strong>${message.sender}</strong> has left the room</p>`;
    else if (message.type === 'GAME_START') messageElement.innerHTML = `<p><strong>${message.sender}</strong> has started a battle with <strong>${message.player2Name || message.opponent}</strong> — ${message.songCount} song${message.songCount !== 1 ? 's' : ''}${message.category ? ` · ${message.category}` : ''}</p>`;
    else if (message.type === 'GAME_END') {
    const winner = message.winnerId
        ? (userList.get(String(message.winnerId)) || message.winnerId)
        : null;
    messageElement.innerHTML = `<p>⚔️ Battle ended! ${winner
        ? `<strong>${winner}</strong> wins — ${message.player1Name} ${message.player1Score} vs ${message.player2Name} ${message.player2Score}`
        : `${message.player1Name} ${message.player1Score} — ${message.player2Name} ${message.player2Score}`
    }</p>`;
}
    else if (message.type === 'QUEUE')       messageElement.innerHTML = `<p><strong>${message.sender}</strong> added <em>${message.songName}</em> to the queue</p>`;
    else if (message.type === 'CHAT')        messageElement.innerHTML = `<strong>${message.sender}</strong><p>${message.content}</p>`;

    if (messageElement.innerHTML) {
        messageBody.appendChild(messageElement);
        messageBody.scrollTop = messageBody.scrollHeight;
    }
}


function sendMessage() {
    const msgContent = messageContent.value.trim();
    if (msgContent && stompClient) {
        stompClient.send(
            `/app/chat/${roomCode}/send`, {},
            JSON.stringify({ sender: username, content: msgContent, type: 'CHAT' })
        );
        messageContent.value = '';
    }
}

function getUserId() {
    for (let [key, value] of userList.entries()) {
        if (value === username) {
            userId = key;
            sessionStorage.setItem("userId", key); 
        }
    }
}


function updateProgress(song, songDur, musicBar) {
    const raw         = song.startedAt;
    const startedAt   = raw.endsWith("Z") ? raw : raw + "Z";
    const elapsedMs   = Date.now() - new Date(startedAt).getTime();
    const remainingMs = Math.max(0, song.duration - elapsedMs);

    const secs = Math.floor(remainingMs / 1000);
    songDur.textContent = `${Math.floor(secs / 60)}:${(secs % 60).toString().padStart(2, "0")}`;
    musicBar.style.width = `${Math.min((elapsedMs / song.duration) * 100, 100)}%`;
}


function updateNowPlaying(song) {
     if (!song?.startedAt) {
        return;
    }

    currentSong = song;

    const battleSongInfo = document.getElementById("battle-song-info");
    if (battleSongInfo) {
        if (song.addedBy) {
            const addedByName = userList.get(String(song.addedBy)) || song.addedBy;
            battleSongInfo.innerHTML = `
                <img src="${song.songBanner || ''}" style="width:40px;height:40px;border-radius:6px;object-fit:cover;${!song.songBanner ? 'display:none' : ''}"/>
                <strong>${song.songName}</strong>
                <span>${song.artist}</span>
                <span>⚔️ ${addedByName}'s pick</span>
            `;
        } else {
            battleSongInfo.textContent = "Waiting for battle song...";
        }
    }

    document.getElementById("trackTitle").textContent  = song.songName || "Nothing playing";
    document.getElementById("trackArtist").textContent = song.artist   || "—";

    const img      = document.getElementById("trackImage");
    const songDur  = document.getElementById("song-duration");
    const musicBar = document.getElementById("music-bar");

    if (song.songLink) songLink = song.songLink;

    clearSongTimers();

    if (img) {
        img.src           = song.songBanner || "";
        img.style.display = song.songBanner ? "block" : "none";
    }

    const raw         = song.startedAt;
    const startedAt   = raw.endsWith("Z") ? raw : raw + "Z";
    const elapsedMs   = Date.now() - new Date(startedAt).getTime();
    const remainingMs = Math.max(0, song.duration - elapsedMs);

    if (remainingMs <= 0) {
        if (musicBar) musicBar.style.width = "100%";
        if (songDur)  songDur.textContent  = "0:00";
        if (isHost()) playNext(song);
        return;
    }

    updateProgress(song, songDur, musicBar);
    progressInterval = setInterval(() => updateProgress(song, songDur, musicBar), 1000);

    endSongTimeout = setTimeout(() => {
        clearSongTimers();
        if (musicBar) musicBar.style.width = "100%";
        if (songDur)  songDur.textContent  = "0:00";
        if (isHost()) playNext(song);
    }, remainingMs);
}

function clearSongTimers() {
    if (progressInterval) { clearInterval(progressInterval);  progressInterval = null; }
    if (endSongTimeout)   { clearTimeout(endSongTimeout);     endSongTimeout   = null; }
}

function openCurrentSong() { if (songLink) window.open(songLink, "_blank"); }
function togglePlay() {
    const btn = document.getElementById("playBtn");
    btn.textContent = btn.textContent === "▶" ? "⏸" : "▶";
}


const setCurrentSong = async (songData) => {
    if (!isHost()) { alert("Only the host can change the current song"); return; }

    const startedAt = new Date().toISOString();
    const payload   = { ...songData, startedAt };

    try {
        const response = await fetch(
            `http://localhost:8080/spotify/updateCurrentlyPlaying?roomCode=${roomCode}`,
            { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) }
        );
        if (!response.ok) throw new Error("Failed to set current song");

        const savedSong = await response.json();
        const canonicalSong = { 
            ...savedSong, 
            startedAt: savedSong.startedAt ?? startedAt,
            addedBy: savedSong.addedBy ?? songData.addedBy  // ← preserve addedBy
        };

        stompClient.send(
            `/app/chat/${roomCode}/music`, {},
            JSON.stringify({ ...canonicalSong, sender: roomHost, type: "START_MUSIC" })
        );

        updateNowPlaying(canonicalSong);

    } catch (err) {
        console.error("setCurrentSong error:", err);
    }
};


const playNext = async (song) => {
    if (!isHost()) return;
    if (!song?.playlistSongId)  return; 

    const startedAt = new Date().toISOString();

    try {

        const response = await fetch(
            `http://localhost:8080/spotify/playNext?songId=${song.playlistSongId}&roomCode=${roomCode}&startedAt=${encodeURIComponent(startedAt)}`,
            { method: "POST" }
        );
        if (!response.ok) { console.error("playNext HTTP error:", response.status); return; }

        const nextSong = await response.json();
        if (!nextSong?.songName) return

        const canonicalSong = { 
            ...nextSong, 
            playlistSongId: nextSong.id, 
            startedAt: nextSong.startedAt ?? startedAt,
            addedBy: nextSong.addedBy ?? null  
        };
        stompClient.send(
            `/app/chat/${roomCode}/music`, {},
            JSON.stringify({ ...canonicalSong, sender: roomHost, type: "START_MUSIC" })
        );

        updateNowPlaying(canonicalSong);

    } catch (err) {
        console.error("playNext error:", err);
    }
};

const playPrev = async (song) => {
    if (!isHost()) return;
    if (!song?.playlistSongId)  return; 

    const startedAt = new Date().toISOString();

    try {

        const response = await fetch(
            `http://localhost:8080/spotify/playPrev?songId=${song.playlistSongId}&roomCode=${roomCode}&startedAt=${encodeURIComponent(startedAt)}`,
            { method: "POST" }
        );
        if (!response.ok) { console.error("playPrev HTTP error:", response.status); return; }

        const prevSong = await response.json();
        if (!prevSong?.songName) return


        const canonicalSong = { 
            ...prevSong, 
            playlistSongId: prevSong.id, 
            startedAt: prevSong.startedAt ?? startedAt,
            addedBy: prevSong.addedBy ?? null  
        };
        stompClient.send(
            `/app/chat/${roomCode}/music`, {},
            JSON.stringify({ ...canonicalSong, sender: roomHost, type: "START_MUSIC" })
        );

        updateNowPlaying(canonicalSong);

    } catch (err) {
        console.error("playPrev error:", err);
    }
};


const getCurrentSong = async () => {
    try {
        const res  = await fetch(`http://localhost:8080/spotify/getCurrentlyPlaying?roomCode=${roomCode}`);
        if (!res.ok) throw new Error("Failed to get current song");
        const song = await res.json();

        if (song?.songName && song?.startedAt) {
            updateNowPlaying(song);
        }
        return song;
    } catch (err) {
        console.error("getCurrentSong error:", err);
    }
};

const getPlaylist = async () => {
    try {
        const res      = await fetch(`http://localhost:8080/spotify/getPlaylist?roomCode=${roomCode}`);
        if (!res.ok) throw new Error("Failed to get playlist");
        const playlist = await res.json();

        playlistState = battleData?.id ? interleaveBattleSongs(playlist) : (playlist || []);

        renderQueue(playlistState);
        return playlist;
    } catch (err) {
        console.error("getPlaylist error:", err);
    }
};

function interleaveBattleSongs(playlist) {
    const player1Songs = playlist.filter(s => s.addedBy != null && String(s.addedBy) === battleData.player1Id);
    const player2Songs = playlist.filter(s => s.addedBy != null && String(s.addedBy) === battleData.player2Id);
    const general      = playlist.filter(s => s.addedBy == null);

    const interleaved = [];
    const max = Math.max(player1Songs.length, player2Songs.length);

    for (let i = 0; i < max; i++) {
        if (i < player1Songs.length) interleaved.push(player1Songs[i]);
        if (i < player2Songs.length) interleaved.push(player2Songs[i]);
    }
    return [...general, ...interleaved];
}

const addSongToPlaylist = async (track) => {
    try {
        const res = await fetch("http://localhost:8080/spotify/addToPlaylist", {
            method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                roomCode, artist: track.artists?.[0]?.name, songName: track.name,
                duration: track.duration_ms, songBanner: track.album?.images?.[0]?.url,
                songLink: track.external_urls?.spotify
            })
        });
        if (!res.ok) throw new Error("Failed to add song");
        stompClient.send(`/app/chat/${roomCode}/queue`, {}, JSON.stringify({
            sender: username, songName: track.name, artist: track.artists?.[0]?.name,
            songBanner: track.album?.images?.[0]?.url, songLink: track.external_urls?.spotify,
            duration: track.duration_ms, battleActive, type: "QUEUE"
        }));
        await getPlaylist();
    } catch (err) { console.error(err); }
};

const addSongToPlaylistForBattle = async (track) => {
    try {
        const res = await fetch(
            `http://localhost:8080/spotify/addSongForUser?roomCode=${roomCode}&userId=${userId}`,
            {
                method: "POST", headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    roomCode, artist: track.artists?.[0]?.name, songName: track.name,
                    duration: track.duration_ms, songBanner: track.album?.images?.[0]?.url,
                    songLink: track.external_urls?.spotify, addedBy: userId
                })
            }
        );

        if (!res.ok) {
            const errorData = await res.json(); 
            const message = errorData.message || "Failed to add song";
            alert(message);
            throw new Error(message);
        }

        stompClient.send(`/app/chat/${roomCode}/queue`, {}, JSON.stringify({
            sender: username, songName: track.name, artist: track.artists?.[0]?.name,
            songBanner: track.album?.images?.[0]?.url, songLink: track.external_urls?.spotify,
            duration: track.duration_ms, battleActive, type: "QUEUE"
        }));
        await getPlaylist();
    } catch (err) { console.error(err); }
};

const deleteSongFromPlaylist = async (song) => {
    if (!isHost()) {
        alert("Only host can remove songs")
        return
    };
    if (!song?.playlistSongId) return

    try {
        const response = await fetch(
            `http://localhost:8080/spotify/deleteSong?songId=${song.playlistSongId}&roomCode=${roomCode}`,
            { method: "POST" }
        );
        if (!response.ok) { console.error("delete song HTTP error:", response.status); return; }

        const playlist = await response.json();


        stompClient.send(
            `/app/chat/${roomCode}/music`, {},
            JSON.stringify({ songs: playlist, sender: roomHost, type: "DELETE_MUSIC" })
        );

        renderQueue(playlist);

    } catch (err) {
        console.error("Delete song error:", err);
    }
};


const getBattle = async () => {
    try {
        const res  = await fetch(`http://localhost:8080/spotify/getBattle?roomCode=${roomCode}`);
        if (!res.ok) throw new Error("Failed to get battle");
        const data = await res.json();
        if (data?.id) {
            battleData = {
                ...data,
                player1Id: data.player1Id != null ? String(data.player1Id) : null,
                player2Id: data.player2Id != null ? String(data.player2Id) : null,
                winnerId:  data.winnerId  != null ? String(data.winnerId)  : null,
                songs: data.songs || []
            };
            showBattleStarted(battleData);
        }
    } catch (err) { console.log("getBattle error:", err); }
};

const startBattle = async () => {
    const songCount  = document.getElementById("song-count").value;
    const liveVoting = document.getElementById("live-voting").value;
    const category   = document.getElementById("battle-category").value;

    if (!selectedOpponent || !songCount) {
        alert("Please select opponent and song count");
        return;
    }

    stompClient.send(
        `/app/chat/${roomCode}/game`, {},
        JSON.stringify({
            sender:      username,
            opponent:    selectedOpponent,
            opponentId:  selectedOpponentId,
            roomCode:    roomCode,
            category:    category,
            songCount:   parseInt(songCount),
            liveVoting:  liveVoting === "yes",
            player1Name: username,
            player2Name: selectedOpponent,
            type: "GAME_START"
        })
    );

    try {
        const res = await fetch("http://localhost:8080/spotify/createBattle", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                roomCode:   roomCode,
                player1Id:  userId,
                gameStatus: 'ACTIVE',
                player2Id:  selectedOpponentId,
                songCount:  parseInt(songCount),
                liveVoting: liveVoting === "yes",
                category:   category,
                startedAt:  new Date().toISOString()
            })
        });

        if (!res.ok) throw new Error("Failed to create battle");

        const data = await res.json();

        console.log("Battle created:", data);
        battleData = {
            id:           data.id,
            roomCode:     data.roomCode,
            player1Id:    data.player1Id != null ? String(data.player1Id) : null,
            player2Id:    data.player2Id != null ? String(data.player2Id) : null,
            songCount:    data.songCount,
            liveVoting:   data.liveVoting,
            gameStatus:   data.gameStatus,
            category:     data.category,
            songs:        data.songs || [],
            startedAt:    data.startedAt,
            endedAt:      data.endedAt,
            player1Score: data.player1Score,
            player2Score: data.player2Score,
            winnerId:     data.winnerId != null ? String(data.winnerId) : null
        };

        document.getElementById("vote-player1").onclick = () => voteUser(data.player1Id);
        document.getElementById("vote-player2").onclick = () => voteUser(data.player2Id);

    } catch (err) {
        console.error("Error in startBattle:", err);
    }

    battleActive = true;
    closeBattlePopup();
};

const endBattle = async () => {
    if (userId !== hostId) { alert("Only host can end battle"); return; }

    try {
        const res = await fetch(`http://localhost:8080/spotify/endBattle?roomCode=${roomCode}`, {
            method: 'POST',
            headers: { "Content-Type": "application/json" }
        });

        if (!res.ok) throw new Error("Failed to end battle");

        const data = await res.json();

        stompClient.send(
            `/app/chat/${roomCode}/game`, {},
            JSON.stringify({
                sender:   username,
                roomCode: roomCode,
                winnerId: data.winnerId,
                player1Name: userList.get(String(data.player1Id)) || data.player1Id,
                player2Name: userList.get(String(data.player2Id)) || data.player2Id,
                player1Score: data.player1Score ?? 0,
                player2Score: data.player2Score ?? 0,
                type: "GAME_END"
            })
        );

        resetBattleUI();

    } catch (err) {
        console.error("endBattle error:", err);
    }
};

function resetBattleUI() {
    battleActive = false;
    battleData = {
        id: null, roomCode: null, player1Id: null, player2Id: null,
        songCount: 0, liveVoting: false, gameStatus: null, songs: [],
        startedAt: null, endedAt: null, player1Score: null, player2Score: null, winnerId: null
    };

    document.getElementById("battle-started-section").classList.add("hidden");
    document.getElementById("live-voting-panel").classList.add("hidden");
    document.getElementById("choose-battle-section").classList.remove("hidden");

    document.getElementById("battle-text").textContent      = "";
    document.getElementById("player1-votes").textContent    = "Player 1: 0 votes";
    document.getElementById("player2-votes").textContent    = "Player 2: 0 votes";
    document.getElementById("vote-player1").textContent     = "Vote Player 1";
    document.getElementById("vote-player2").textContent     = "Vote Player 2";
    document.getElementById("battle-end").textContent       = "End Battle";
}

const voteUser = async (playerId) => {
    try {
        const res = await fetch(`http://localhost:8080/spotify/votePlayer?roomCode=${roomCode}`,
            { method: 'POST', headers: { "Content-Type": "application/json" }, body: JSON.stringify({ playerId }) });

        if (!res.ok) { console.error("voteUser HTTP error:", res.status); return; }

        const battle = await res.json();

        stompClient.send(
            `/app/chat/${roomCode}/game`, {},
            JSON.stringify({
                sender:       username,
                roomCode:     roomCode,
                player1Id:    battle.player1Id,
                player2Id:    battle.player2Id,
                player1Score: battle.player1Score,
                player2Score: battle.player2Score,
                type:         "VOTE"
            })
        );

    } catch (err) { console.error(err); }
};

function updateBattleVotes(battle) {
    const p1 = userList.get(String(battle.player1Id)) || battle.player1Id;
    const p2 = userList.get(String(battle.player2Id)) || battle.player2Id;
    document.getElementById("player1-votes").textContent = `${p1}: ${battle.player1Score || 0} votes`;
    document.getElementById("player2-votes").textContent = `${p2}: ${battle.player2Score || 0} votes`;
}

function showBattleStarted(battle) {
    battleActive = true;

    if (battle.id) {
        battleData = { ...battleData, ...battle, songs: battle.songs || battleData.songs || [] };
    }

    const player1Id = battle.player1Id != null ? String(battle.player1Id) : (battle.sender === username ? userId : null);
    const player2Id = battle.player2Id != null ? String(battle.player2Id) : (battle.opponentId != null ? String(battle.opponentId) : null);

    const player1Name = userList.get(player1Id) || battle.player1Name || battle.sender || player1Id;
    const player2Name = userList.get(player2Id) || battle.player2Name || battle.opponent || player2Id;

    const player1Btn = document.getElementById("vote-player1");
    const player2Btn = document.getElementById("vote-player2");

    document.getElementById("live-voting-panel").classList.remove("hidden");

    player1Btn.textContent = `Vote ${player1Name}`;
    player2Btn.textContent = `Vote ${player2Name}`;

    player1Btn.onclick = () => voteUser(player1Id);
    player2Btn.onclick = () => voteUser(player2Id);

    document.getElementById("player1-votes").textContent = `${player1Name}: ${battle.player1Score || 0} votes`;
    document.getElementById("player2-votes").textContent = `${player2Name}: ${battle.player2Score || 0} votes`;

    document.getElementById("choose-battle-section").classList.add("hidden");
    document.getElementById("battle-started-section").classList.remove("hidden");
    document.getElementById("battle-text").textContent = `${player1Name} vs ${player2Name} has begun add your songs!`;
    document.getElementById("battle-end").textContent = `End Battle`;
    document.getElementById("battle-end").onclick = () => endBattle();
}

function openBattlePopup(opponentUsername, opponentUserId) {
    selectedOpponent   = opponentUsername;
    selectedOpponentId = opponentUserId;
    document.getElementById("selected-opponent").textContent = opponentUsername;
    document.getElementById("battle-popup").classList.remove("hidden");
    validateBattleForm();
}

function closeBattlePopup() {
    document.getElementById("battle-popup").classList.add("hidden");
    selectedOpponent = selectedOpponentId = null;
    document.getElementById("start-battle-btn").disabled = true;
}

function validateBattleForm() {
    const songCount = document.getElementById("song-count").value;
    document.getElementById("start-battle-btn").disabled = !(selectedOpponent && songCount);
}

function battleUser(event) {
    const el = event.target;
    if (!el.classList.contains("users-list")) return;
    openBattlePopup(el.dataset.username, el.dataset.userId);
}

function canQueue() {
    if (!battleData?.id) return true;
    return userId === battleData.player1Id || userId === battleData.player2Id;
}

const renderQueue = (playlist) => {
    if (!playlist) return;
    queueList.innerHTML = "";

    const currentUserId = userId || sessionStorage.getItem("userId");

    playlist.forEach(song => {
        const addedById = song.addedBy != null ? String(song.addedBy) : null;
        const addedByName = addedById
            ? (userList.get(addedById) || (addedById === currentUserId ? username : addedById))
            : null;
        const isMyBattleSong = addedById && addedById === currentUserId;

        const item = document.createElement("div");
        item.classList.add("queue-item");
        if (addedById) item.classList.add("battle-song");
        if (isMyBattleSong) item.classList.add("my-battle-song");

        item.innerHTML = `
            <div class="queue-info">
                <div class="queue-text">
                    <div class="queue-title">${song.songName}</div>
                    <div class="queue-artist">${song.artist}</div>
                    ${addedByName ? `<div class="queue-added-by">⚔️ ${addedByName}</div>` : ''}
                </div>
                <button class="delete-play-btn">Remove</button>
                <button class="queue-play-btn">▶</button>
            </div>`;

        item.querySelector(".delete-play-btn").onclick = (e) => {
            e.stopPropagation();
            deleteSongFromPlaylist({ ...song, playlistSongId: song.id });
        };
        item.querySelector(".queue-play-btn").onclick = (e) => {
            e.stopPropagation();
            if (!isHost()) { alert("Only the host can play songs from the queue"); return; }
            setCurrentSong({
                playlistSongId: song.id,
                artist:     song.artist     ?? song.artists?.[0]?.name,
                songName:   song.songName   ?? song.name,
                duration:   song.duration   ?? song.duration_ms,
                songBanner: song.songBanner ?? song.album?.images?.[0]?.url,
                songLink:   song.songLink   ?? song.external_urls?.spotify,
                addedBy:    song.addedBy ?? null 

            });
        };

        queueList.appendChild(item);
    });
};

function renderDropdown(tracks) {
    const dropdown = document.getElementById("music-dropdown");
    dropdown.innerHTML = "";
    tracks.forEach(track => {
        const item = document.createElement("div");
        item.classList.add("music-item");
        item.innerHTML = `
            <img src="${track?.album?.images?.[0]?.url}" />
            <button class="play-btn">▶</button>
            <div class="music-text">
                <div class="music-title">${track.name}</div>
                <div class="music-artist">${track.artists?.[0]?.name}</div>
            </div>
            <button class="queue-btn">Add to queue</button>`;

        item.onclick = () => {
            if (!isHost()) { alert("Only the host can play music. Add it to the queue instead."); return; }
            setCurrentSong({
                playlistSongId: null,
                artist:     track.artists?.[0]?.name,
                songName:   track.name,
                duration:   track.duration_ms,
                songBanner: track.album?.images?.[0]?.url,
                songLink:   track.external_urls?.spotify
            });
            dropdown.classList.add("hidden");
        };

        item.querySelector(".queue-btn").onclick = async (e) => {
            e.stopPropagation();
            if (!canQueue()) { alert("You are not part of this battle"); return; }
            battleActive ? await addSongToPlaylistForBattle(track) : await addSongToPlaylist(track);
        };

        dropdown.appendChild(item);
    });
    dropdown.classList.remove("hidden");
}



const searchMusic = async () => {
    try {
        const query = document.getElementById("music-search").value;
        const res   = await fetch(`http://localhost:8080/spotify/track?param=${encodeURIComponent(query)}&roomCode=${roomCode}`);
               if (!res.ok) {
            const errorText = await res.text();

            console.error("Status:", res.status);
            console.error("Response:", errorText);

            throw new Error(
                `Request failed (${res.status}): ${errorText}`
            );
        }
        renderDropdown((await res.json()).tracks.items);
    } catch (err) { console.log("searchMusic error:", err); }
};

const deleteRoom = async () => {
    if (hostId !== userId) {
        alert("Only host can end room");
        return;
    }
    try {
        await fetch(`http://localhost:8080/spotify/deleteRoom?roomCode=${roomCode}&hostId=${hostId}`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }
        });

        stompClient.send(
            `/app/chat/${roomCode}/send`, {},
            JSON.stringify({ sender: username, type: 'DELETE_ROOM' })
        );

        window.location.href = '/';
    } catch (err) {
        console.log("deleteRoom error:", err);
    }
};

const onLeave = async () => {
    
    try {
        await fetch(`http://localhost:8080/spotify/deleteRoom?roomCode=${roomCode}&hostId=${hostId}`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }
        });

        stompClient.send(
            `/app/chat/${roomCode}/send`, {},
            JSON.stringify({ sender: username, type: 'DELETE_ROOM' })
        );

        window.location.href = '/';
    } catch (err) {
        console.log("deleteRoom error:", err);
    }
};


document.addEventListener("click", (e) => {
    const dropdown = document.getElementById("music-dropdown");
    const search   = document.getElementById("music-search");
    if (!search.contains(e.target) && !dropdown.contains(e.target)) dropdown.classList.add("hidden");
});

battleUsers.addEventListener('click', battleUser);
messageContent.addEventListener("keypress", (e) => { if (e.key === "Enter") sendMessage(); });

document.getElementById("playNext").addEventListener("click", () => {
    if (!isHost()) { alert("Only the host can skip songs"); return; }
    if (!currentSong) return
    clearSongTimers();
    playNext(currentSong);
});
document.getElementById("playPrev").addEventListener("click", () => {
    if (!isHost()) { alert("Only the host can go back to songs"); return; }
    if (!currentSong) return
    clearSongTimers();
    playPrev(currentSong);
});

connect();