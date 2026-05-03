package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.service.SystemSettingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = "http://localhost:5173")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    public SystemSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    @GetMapping("/booking-window-days")
    public Map<String, Integer> getBookingWindowDays() {
        return Map.of("bookingWindowDays", systemSettingService.getBookingWindowDays());
    }

    @PutMapping("/booking-window-days")
    @RequireRole(roles = "ADMIN")
    public Map<String, Integer> updateBookingWindowDays(@RequestBody Map<String, Integer> body) {
        Integer days = body.get("bookingWindowDays");
        if (days == null || days < 1) {
            days = 1;
        }
        return Map.of("bookingWindowDays", systemSettingService.updateBookingWindowDays(days).getBookingWindowDays());
    }

    @GetMapping("/ai-config")
    @RequireRole(roles = "ADMIN")
    public Map<String, Object> getAiConfig() {
        var setting = systemSettingService.getSystemSetting();
        return Map.of(
            "aiApiKey", setting.getAiApiKey() != null ? "***" : "",
            "aiModel", setting.getAiModel() != null ? setting.getAiModel() : "",
            "aiBaseUrl", setting.getAiBaseUrl() != null ? setting.getAiBaseUrl() : "",
            "aiEnabled", setting.getAiEnabled() != null ? setting.getAiEnabled() : false
        );
    }

    @PutMapping("/ai-config")
    @RequireRole(roles = "ADMIN")
    public Map<String, Object> updateAiConfig(@RequestBody Map<String, Object> body) {
        String apiKey = (String) body.get("aiApiKey");
        String model = (String) body.get("aiModel");
        String baseUrl = (String) body.get("aiBaseUrl");
        Boolean enabled = (Boolean) body.get("aiEnabled");
        
        var setting = systemSettingService.getSystemSetting();
        
        // 只在提供新密钥时更新
        if (apiKey != null && !apiKey.isEmpty() && !apiKey.equals("***")) {
            setting.setAiApiKey(apiKey);
        }
        
        if (model != null && !model.isEmpty()) {
            setting.setAiModel(model);
        }
        
        if (baseUrl != null && !baseUrl.isEmpty()) {
            setting.setAiBaseUrl(baseUrl);
        }
        
        if (enabled != null) {
            setting.setAiEnabled(enabled);
        }
        
        setting = systemSettingService.updateSystemSetting(setting);
        
        return Map.of(
            "aiApiKey", "***",
            "aiModel", setting.getAiModel() != null ? setting.getAiModel() : "",
            "aiBaseUrl", setting.getAiBaseUrl() != null ? setting.getAiBaseUrl() : "",
            "aiEnabled", setting.getAiEnabled() != null ? setting.getAiEnabled() : false
        );
    }
}
