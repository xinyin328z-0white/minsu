package com.example.minsumgr.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * AI生成订单草稿响应
 */
@Data
public class BookingDraftResponse {
    private boolean success;
    private String message;
    private List<BookingOption> options;
    private String aiExplanation;
    
    @Data
    public static class BookingOption {
        private String optionId; // 用于前端标识选项
        private Long homestayId;
        private String homestayName;
        private String homestayLocation;
        private String homestayImageUrl;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private Integer nights;
        private BigDecimal pricePerNight;
        private BigDecimal totalPrice;
        private Integer guestCount;
        private Integer availableRooms;
        private Boolean isAvailable; // 是否可用（匹配JSON字段名）
        private String roomNotes;
        private String recommendReason; // AI推荐理由
    }
}
