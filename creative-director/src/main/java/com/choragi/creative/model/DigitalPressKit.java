package com.choragi.creative.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DigitalPressKit {
    private String artistName;
    private String theme;
    private String posterBase64;
    private String posterGcsUrl;
    private String status;
}