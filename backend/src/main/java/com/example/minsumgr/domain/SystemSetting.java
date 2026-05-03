package com.example.minsumgr.domain;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer bookingWindowDays;
    
    // AI 服务配置
    private String aiApiKey;
    
    private String aiModel; // gpt-3.5-turbo, gpt-4, etc.
    
    private String aiBaseUrl; // API endpoint URL
    
    private Boolean aiEnabled; // 是否启用AI服务
}
