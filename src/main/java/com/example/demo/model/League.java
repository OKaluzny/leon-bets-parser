package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class League {
    private long id;
    private String name;
    private boolean top;
    private int topOrder;
    private int prematch;
    private Region region;
    private Sport sport;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isTop() { return top; }
    public void setTop(boolean top) { this.top = top; }
    public int getTopOrder() { return topOrder; }
    public void setTopOrder(int topOrder) { this.topOrder = topOrder; }
    public int getPrematch() { return prematch; }
    public void setPrematch(int prematch) { this.prematch = prematch; }
    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }
    public Sport getSport() { return sport; }
    public void setSport(Sport sport) { this.sport = sport; }
}
