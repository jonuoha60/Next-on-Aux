 import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
 import {
    getAuth,
    GoogleAuthProvider,
    signInWithPopup
  } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-auth.js";

  const firebaseConfig = {
    apiKey: "YOUR_API_KEY",
    authDomain: "YOUR_PROJECT.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    appId: "YOUR_APP_ID"
  };

  const app = initializeApp(firebaseConfig);
  const auth = getAuth(app);
  const provider = new GoogleAuthProvider();

  const spotifyBtn = document.querySelector(".spotify-btn");

  spotifyBtn.addEventListener("click", async () => {
    try {
      const result = await signInWithPopup(auth, provider);
      const user = result.user;

      console.log("Logged in user:", user);

      const token = await user.getIdToken();

      const response = await fetch("http://localhost:8080/auth/google", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": "Bearer " + token
        }
      });

      if (response.ok) {
        window.location.href = "/home";
      }

    } catch (err) {
      console.error("Login failed:", err);
    }
  });