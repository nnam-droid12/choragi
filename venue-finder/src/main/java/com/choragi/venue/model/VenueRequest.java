package com.choragi.venue.model;

import lombok.Data;

@Data
public class VenueRequest {
    private String artistId;
    private String targetCity;
    private String artistProfile;
}