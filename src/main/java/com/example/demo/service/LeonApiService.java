package com.example.demo.service;

import com.example.demo.config.LeonApiProperties;
import com.example.demo.model.Event;
import com.example.demo.model.EventsResponse;
import com.example.demo.model.Sport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.Collections;
import java.util.List;

@Service
public class LeonApiService {

    private static final Logger log = LoggerFactory.getLogger(LeonApiService.class);

    private static final String CTAG = "en-US";
    private static final String FLAGS = "reg,urlv2,mm2,rrc,nodup";

    private final WebClient webClient;
    private final Retry retrySpec;

    public LeonApiService(WebClient webClient, LeonApiProperties properties) {
        this.webClient = webClient;
        this.retrySpec = Retry.backoff(
                properties.api().retry().maxAttempts(),
                properties.api().retry().delay()
        ).filter(this::isRetryableException);
    }

    public Mono<List<Sport>> getSports() {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api-2/betline/sports")
                        .queryParam("ctag", CTAG)
                        .queryParam("flags", "urlv2")
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ApiException("Failed to fetch sports: " + response.statusCode())))
                .bodyToMono(new ParameterizedTypeReference<List<Sport>>() {})
                .retryWhen(retrySpec)
                .doOnError(e -> log.error("Error fetching sports", e))
                .onErrorReturn(Collections.emptyList());
    }

    public Mono<EventsResponse> getEventsByLeague(long leagueId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api-2/betline/events/all")
                        .queryParam("ctag", CTAG)
                        .queryParam("league_id", leagueId)
                        .queryParam("hideClosed", "true")
                        .queryParam("flags", FLAGS)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ApiException("Failed to fetch events for league " + leagueId)))
                .bodyToMono(EventsResponse.class)
                .retryWhen(retrySpec)
                .doOnError(e -> log.warn("Error fetching events for league {}: {}", leagueId, e.getMessage()))
                .onErrorReturn(new EventsResponse());
    }

    public Mono<Event> getEventDetails(long eventId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api-2/betline/event/all")
                        .queryParam("ctag", CTAG)
                        .queryParam("eventId", eventId)
                        .queryParam("flags", FLAGS)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::isError, response ->
                        Mono.error(new ApiException("Failed to fetch event " + eventId)))
                .bodyToMono(Event.class)
                .retryWhen(retrySpec)
                .doOnError(e -> log.warn("Error fetching event {}: {}", eventId, e.getMessage()))
                .onErrorResume(e -> Mono.empty());
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException wcre) {
            int status = wcre.getStatusCode().value();
            return status >= 500 || status == 429;
        }
        return throwable instanceof ApiException;
    }

    public static class ApiException extends RuntimeException {
        public ApiException(String message) {
            super(message);
        }
    }
}
