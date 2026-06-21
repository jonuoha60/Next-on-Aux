package com.example.demo.service;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class SpotifyService {

    private final WebClient apiClient;

    public SpotifyService(WebClient apiClient) {
      this.apiClient = apiClient;
    }

    public String searchTracks(String q, String accessToken) {
    System.out.println("Query = [" + q + "]");
    System.out.println("Token length = " +
            (accessToken == null ? 0 : accessToken.length()));
        return apiClient.get()
            .uri(uriBuilder -> uriBuilder
                    .path("/search")
                    .queryParam("type", "track")
                    .queryParam("q", q)
                    .build())
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .bodyToMono(String.class)
            .block();
}
    
}
