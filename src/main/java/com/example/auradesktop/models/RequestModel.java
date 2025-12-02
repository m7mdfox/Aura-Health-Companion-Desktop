package com.example.auradesktop.models; // Or whatever package you prefer

public class RequestModel {
    private String name;
    private String time;

    public RequestModel(String name, String time) {
        this.name = name;
        this.time = time;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}