package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventsResponse(
        List<Event> events
) {
    public EventsResponse {
        if (events == null) {
            events = List.of();
        }
    }

    public EventsResponse() {
        this(List.of());
    }
}
