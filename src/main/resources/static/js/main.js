'use strict';

const usernameCreate = document.getElementById("usernameCreate");
const roomCreate = document.getElementById("roomNameCreate");
const maxCreate = document.getElementById("maxCreate");
const createForm = document.getElementById("create")
const sendMessageForm = document.getElementById("send-message")
const messageContent = document.getElementById("message-content")
const messageBody = document.getElementById("message-body")
const roomCodeJoin = document.getElementById("room-code-join")
const roomNameJoin = document.getElementById("room-name-join")
const usernameJoin = document.getElementById("username-join")

let stompClient = null;
let username = null;
let roomName = null;
let roomCode;
let maxPlayer = null;
let errors = []

function validateForm(username, roomName, maxPlayer) {
    errors = []; 

    if (roomName.trim() == "") {
        errors.push("Room name can't be empty");
    } else if (roomName.length <= 3) {
        errors.push("Room name can't be less than 4 characters");
    }

    if (username.trim() == "") {
        errors.push("Username can't be empty");
    } else if (username.length <= 3) {
        errors.push("Username can't be less than 4 characters");
    }

    if (maxPlayer.trim() == "") {
        errors.push("Max players can't be empty");
    } else if (maxPlayer <= 1) {
        errors.push("Max players can't be less than 2");
    }

    return errors.length === 0;
}

function showErrors() {
    const box = document.getElementById("error-box");
    box.innerHTML = "";

    if (errors.length === 0) return;

    const ul = document.createElement("ul");

    errors.forEach(err => {
        const li = document.createElement("li");
        li.classList.add("error-list")
        li.textContent = err;
        ul.appendChild(li);
    });

    box.appendChild(ul);
}



function onCreate(event) {
    event.preventDefault(); 

    username = document.getElementById("usernameCreate").value.trim();
    roomName = document.getElementById("roomNameCreate").value.trim();
    maxPlayer = document.getElementById("maxCreate").value.trim();

    if (!validateForm(username, roomName, maxPlayer)) {
        showErrors(); 
        return;
    }

    document.getElementById("error-box").innerHTML = ""; 

    sessionStorage.setItem("username", username);
    
    document.getElementById("create").submit();

}

function onJoin() {

    const roomCode = roomCodeJoin.value
    const roomName = roomNameJoin.value
    const username = usernameJoin.value.trim()
    
    sessionStorage.setItem("username", username)
     
}


const toggle = document.getElementById("privateToggle");
const roomTypeText = document.getElementById("roomTypeText");

toggle.addEventListener("change", () => {
    roomTypeText.textContent = toggle.checked
        ? "Private"
        : "Public";
});

