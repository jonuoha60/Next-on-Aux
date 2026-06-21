package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.helper.SecurityUtils;
import com.example.demo.model.User;
import com.example.demo.repo.UserRepository;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {

    private final JWTService jwtService;
    private final UserRepository userRepository;
    private final String refreshSecret;

    @Autowired
    public AuthService(
            JWTService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;

        Dotenv dotenv = Dotenv.load();
        this.refreshSecret = dotenv.get("REFRESH_TOKEN_SECRET");
    }

    public User getCurrentUser(
            HttpServletRequest request,
            HttpSession session) {

        User user = (User) session.getAttribute("user");

        if (user != null) {
            return user;
        }

        String refreshToken =
                SecurityUtils.getCookieValue(request, "refreshToken");

        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        try {

            String email = jwtService.extractEmail(
                    refreshToken,
                    refreshSecret
            );

            User dbUser = userRepository.findByEmail(email);

            if (dbUser != null) {
                session.setAttribute("user", dbUser);
            }

            return dbUser;

        } catch (Exception e) {
            return null;
        }
    }
}