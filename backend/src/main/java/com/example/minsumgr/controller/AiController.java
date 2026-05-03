package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.dto.RecommendationRequest;
import com.example.minsumgr.service.AiService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:5173")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/recommendations")
    @RequireRole
    public Map<String, String> recommend(@RequestBody RecommendationRequest request) {
        String result = aiService.recommendForUser(request.getUserId());
        return Map.of("recommendation", result);
    }
}
