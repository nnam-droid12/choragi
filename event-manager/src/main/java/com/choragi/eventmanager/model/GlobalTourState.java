package com.choragi.eventmanager.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class GlobalTourState {
    private String tourId;
    private String artistId;
    private String targetCity;
    private String status;


    private String discoveredVenues;

    private List<String> executionLogs = new ArrayList<>();

    public void log(String message) {
        this.executionLogs.add(System.currentTimeMillis() + ": " + message);
    }
}