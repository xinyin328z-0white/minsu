package com.example.minsumgr.dto;

import lombok.Data;

@Data
public class TravelRecommendationRequest {
    private Integer days;  // 旅行天数
    private Integer startDate;  // 开始日期 (相对今天的天数)
    private String customPrompt;  // 自定义提示词
    private Long userId;
}
