package com.example.minsumgr.dto;

import lombok.Data;

@Data
public class TravelRecommendationResponse {
    private String recommendation;  // 旅行计划推荐
    private String homestayAdvice;  // 民宿建议
    private String itinerary;  // 行程规划
}
