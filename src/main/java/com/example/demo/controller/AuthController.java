package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.helper.SecurityUtils;
import com.example.demo.model.User;
import com.example.demo.repo.RefreshTokenRepository;
import com.example.demo.service.JWTService;
import com.example.demo.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;



@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JWTService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    AuthController(UserService userService, JWTService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }
    

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestHeader(value = "Authorization", required = false) String authHeader, HttpSession session) {

        System.out.println("🔥 Google login request received");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("Missing or invalid Authorization header");
            return ResponseEntity.badRequest().body("Missing Authorization header");
        }

        System.out.println("📌 Raw Auth Header: " + authHeader);

        String token = authHeader.replace("Bearer ", "");

        FirebaseToken decoded;

        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(token);
            System.out.println("Firebase token verified successfully");
        } catch (Exception e) {
            System.out.println("Firebase token verification FAILED");
            e.printStackTrace();
            return ResponseEntity.status(401).body("Invalid Firebase token");
        }

        String email = decoded.getEmail();
        String uid = decoded.getUid();
        String username = decoded.getName();
        String photoUrl = decoded.getPicture();

        if (username == null || username.trim().isEmpty()) {
            username = email != null ? email.split("@")[0] : "unknown_user";
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setFireBaseUid(uid);
        user.setPhotoUrl(photoUrl);

        try {
            User savedUser = userService.saveGoogleUser(user);
            session.setAttribute("user", savedUser);

            return ResponseEntity.ok(savedUser);

        } catch (Exception e) {
            System.out.println("Error saving user");
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to save user");
        }
    }

    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("loggedIn", false);
            response.put("user", null);

            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("loggedIn", true);
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpSession session) {

        String refreshToken = SecurityUtils.getCookieValue(request, "refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.status(401).body("No refresh token");
        }

        Dotenv dotenv = Dotenv.load();
        String refreshSecret = dotenv.get("REFRESH_TOKEN_SECRET");

        String email = jwtService.extractEmail(refreshToken, refreshSecret);

        if (email == null) {
            return ResponseEntity.status(401).body("Invalid token");
        }

        User user = userService.findByEmail(email);

        if (user == null) {
            return ResponseEntity.status(404).body("User not found");
        }

        // 🔥 THIS IS WHAT YOU ARE MISSING
        session.setAttribute("user", user);

        Map<String, Object> response = new HashMap<>();
        response.put("loggedIn", true);
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

}
