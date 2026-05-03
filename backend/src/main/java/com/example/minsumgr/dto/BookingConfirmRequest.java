package com.example.minsumgr.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 确认AI生成订单的请求
 */
@Data
public class BookingConfirmRequest {
    private Long userId;
    private Long homestayId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guestCount;
    private String contactPhone;
    private String specialRequests;
    private BigDecimal totalPrice;
}
