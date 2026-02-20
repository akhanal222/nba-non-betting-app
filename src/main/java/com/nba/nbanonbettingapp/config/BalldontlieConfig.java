package com.nba.nbanonbettingapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BalldontlieConfig {
    @Bean
    public RestClient balldontlieRestClient(
            @Value("${balldontlie.base-url}") String baseUrl,
            @Value("${balldontlie.api-key}") String apiKey
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", apiKey)
                .build();
    }
}
