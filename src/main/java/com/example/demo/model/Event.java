package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Event(
        long id,
        String name,
        long kickoff,
        List<Market> markets
) { }
