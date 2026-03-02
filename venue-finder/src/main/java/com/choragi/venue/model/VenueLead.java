package com.choragi.venue.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueLead {
    private String name;
    private String address;
    private String reasoning;
    private String status = "PENDING"; // Default status for negotiation
}