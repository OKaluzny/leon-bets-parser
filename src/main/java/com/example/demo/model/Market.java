package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Market {
    private long id;
    private String name;
    private boolean open;
    private List<Runner> runners;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
    public List<Runner> getRunners() { return runners; }
    public void setRunners(List<Runner> runners) { this.runners = runners; }
}
