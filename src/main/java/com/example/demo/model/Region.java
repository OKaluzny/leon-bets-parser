package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Region {
    private long id;
    private String name;
    private String family;
    private List<League> leagues;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }
    public List<League> getLeagues() { return leagues; }
    public void setLeagues(List<League> leagues) { this.leagues = leagues; }
}
