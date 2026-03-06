package com.choragi.sitebuilder.model;

import lombok.Data;

@Data
public class SiteRequest {
    private String artistName;
    private String theme;
    private String date;
    private String location;
    private String posterUrl;
    private String videoUrl;
}