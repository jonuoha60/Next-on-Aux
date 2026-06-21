package com.example.demo.service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JWTService {

    private static final long ACCESS_EXPIRATION = 1000 * 60 * 30; // 30 minutes
    private static final long REFRESH_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7 days



    public String generateAccessToken(String email, String jwtAccessSecret) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtAccessSecret));
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String email, String jwtRefreshSecret) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtRefreshSecret));
        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token, String refreshSecret) {

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(refreshSecret));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String email = claims.get("email", String.class);

        return email != null ? email : claims.getSubject();
    }
}
