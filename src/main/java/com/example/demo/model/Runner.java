package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Runner {
    private long id;
    private String name;
    private double price;
    private String priceStr;
    private boolean open;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public String getPriceStr() { return priceStr; }
    public void setPriceStr(String priceStr) { this.priceStr = priceStr; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
}
