package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sport {
    private long id;
    private String name;
    private String family;
    private List<Region> regions;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFamily() { return family; }
    public void setFamily(String family) { this.family = family; }
    public List<Region> getRegions() { return regions; }
    public void setRegions(List<Region> regions) { this.regions = regions; }
}
