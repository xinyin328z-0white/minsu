package com.example.minsumgr.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPreferenceRequest {
    private String budgetLevel;
    private List<String> travelStyles;
    private List<String> preferredLocations;
    private Integer minPrice;
    private Integer maxPrice;
    private String companionType;
    private String notes;
}
