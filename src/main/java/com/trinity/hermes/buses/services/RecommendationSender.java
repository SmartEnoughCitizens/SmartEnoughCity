package com.trinity.hermes.buses.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RecommendationSender {

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendAsync(String data) {
        // call python engine here
        restTemplate.postForObject(
                "http://localhost:8000/recommend",
                data,
                Void.class
        );
    }
}
