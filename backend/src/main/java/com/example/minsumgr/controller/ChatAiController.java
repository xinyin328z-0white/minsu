package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.service.ChatAiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 客服AI助手接口
 */
@RestController
@RequestMapping("/api/chat/ai")
public class ChatAiController {

    private final ChatAiService chatAiService;

    public ChatAiController(ChatAiService chatAiService) {
        this.chatAiService = chatAiService;
    }

    /**
     * 获取AI客服回复建议
     * @param userId 用户ID
     * @param conversationContext 对话上下文
     * @return AI建议回复
     */
    @PostMapping("/suggest-reply")
    @RequireRole(roles = "ADMIN")
    public ResponseEntity<Map<String, String>> suggestReply(
            @RequestParam Long userId,
            @RequestBody Map<String, String> request) {
        
        try {
            String conversationContext = request.getOrDefault("context", "");
            String reply = chatAiService.getCustomerServiceReply(userId, conversationContext);
            
            Map<String, String> response = new HashMap<>();
            response.put("suggestedReply", reply);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "AI服务错误: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 查询用户订单
     * @param userId 用户ID
     * @return 订单列表JSON
     */
    @GetMapping("/query-bookings/{userId}")
    @RequireRole(roles = "ADMIN")
    public ResponseEntity<Map<String, Object>> queryUserBookings(@PathVariable Long userId) {
        try {
            String result = chatAiService.queryUserBookings(userId);
            Map<String, Object> response = new HashMap<>();
            
            // 尝试解析结果
            if (result.startsWith("[")) {
                // 如果是JSON数组
                response.put("success", true);
                response.put("data", result);
            } else if (result.startsWith("{")) {
                // 如果是JSON对象
                response.put("success", true);
                response.put("data", result);
            } else {
                // 否则是纯文本消息
                response.put("success", true);
                response.put("message", result);
                response.put("data", "[]");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 查询民宿可用房间
     * @param homestayId 民宿ID
     * @return 房间信息JSON
     */
    @GetMapping("/query-rooms/{homestayId}")
    @RequireRole(roles = "ADMIN")
    public ResponseEntity<Map<String, Object>> queryAvailableRooms(@PathVariable Long homestayId) {
        try {
            String result = chatAiService.queryAvailableRooms(homestayId);
            Map<String, Object> response = new HashMap<>();
            
            if (result.startsWith("{")) {
                response.put("success", true);
                response.put("data", result);
            } else {
                response.put("success", true);
                response.put("message", result);
                response.put("data", "{}");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 查询日期范围内的预订
     * @param homestayId 民宿ID
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 预订情况JSON
     */
    @GetMapping("/query-bookings-by-date")
    @RequireRole(roles = "ADMIN")
    public ResponseEntity<Map<String, Object>> queryBookingsByDateRange(
            @RequestParam Long homestayId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        
        try {
            String result = chatAiService.queryBookingsByDateRange(homestayId, startDate, endDate);
            Map<String, Object> response = new HashMap<>();
            
            if (result.startsWith("{")) {
                response.put("success", true);
                response.put("data", result);
            } else {
                response.put("success", true);
                response.put("message", result);
                response.put("data", "{}");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
