package com.example.minsumgr.dto;

import com.example.minsumgr.domain.UserPreference;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ForYouRecommendationResponse {
    private Boolean preferenceMissing;
    private UserPreference preference;
    private String algorithm;
    private String summary;
    private List<HomestayRecommendation> homestays;
    private List<RouteRecommendation> routes;

    @Data
    public static class HomestayRecommendation {
        private Long id;
        private String name;
        private String location;
        private String description;
        private BigDecimal pricePerNight;
        private Integer availableRooms;
        private String imageUrl;
        private Double score;
        private String reason;
    }

    @Data
    public static class RouteRecommendation {
        private Long id;
        private String name;
        private String description;
        private String tags;
        private Double score;
        private String reason;
    }
}
