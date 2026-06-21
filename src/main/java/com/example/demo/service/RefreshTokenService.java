package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.model.RefreshToken;
import com.example.demo.repo.RefreshTokenRepository;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;

    }

    public void saveToken(RefreshToken refreshToken) {
        refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken getRefresh(String token) {
        RefreshToken refresh = refreshTokenRepository.findByToken(token);
        return refresh;
    }
}
