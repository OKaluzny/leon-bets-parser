package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    private long id;
    private String name;
    private long kickoff;
    private League league;
    private List<Market> markets;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public long getKickoff() { return kickoff; }
    public void setKickoff(long kickoff) { this.kickoff = kickoff; }
    public League getLeague() { return league; }
    public void setLeague(League league) { this.league = league; }
    public List<Market> getMarkets() { return markets; }
    public void setMarkets(List<Market> markets) { this.markets = markets; }
}
