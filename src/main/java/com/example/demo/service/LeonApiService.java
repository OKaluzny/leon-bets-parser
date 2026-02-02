package com.example.demo.service;

import com.example.demo.model.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class LeonApiService {

    private final WebClient webClient;

    public LeonApiService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<Sport>> getSports() {
        return webClient.get()
                .uri("/api-2/betline/sports?ctag=en-US&flags=urlv2")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Sport>>() {});
    }

    public Mono<EventsResponse> getEventsByLeague(long leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api-2/betline/events/all")
                        .queryParam("ctag", "en-US")
                        .queryParam("league_id", leagueId)
                        .queryParam("hideClosed", "true")
                        .queryParam("flags", "reg,urlv2,mm2,rrc,nodup")
                        .build())
                .retrieve()
                .bodyToMono(EventsResponse.class);
    }

    public Mono<Event> getEventDetails(long eventId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api-2/betline/event/all")
                        .queryParam("ctag", "en-US")
                        .queryParam("eventId", eventId)
                        .queryParam("flags", "reg,urlv2,mm2,rrc,nodup")
                        .build())
                .retrieve()
                .bodyToMono(Event.class);
    }
}
