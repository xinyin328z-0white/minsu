package com.example.minsumgr.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI生成订单草稿请求
 */
@Data
public class BookingDraftRequest {
    private Long userId;
    private Integer days;
    private Integer startDate; // 从今天开始计算的天数
    private String budget;
    private String customPrompt;
}
