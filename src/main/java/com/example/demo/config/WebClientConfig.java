package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
 
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    public WebClient apiClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl("https://api.spotify.com/v1")
                .build();
    }
}
