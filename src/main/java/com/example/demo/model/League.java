package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record League(
        long id,
        String name,
        boolean top,
        int topOrder,
        int prematch
) {}
