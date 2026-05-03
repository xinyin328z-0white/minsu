package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.domain.UserPreference;
import com.example.minsumgr.dto.ForYouRecommendationResponse;
import com.example.minsumgr.dto.UserPreferenceRequest;
import com.example.minsumgr.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "http://localhost:5173")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/preferences/{userId}")
    @RequireRole
    public Map<String, Object> getPreference(@PathVariable Long userId) {
        return recommendationService.getPreference(userId)
                .<Map<String, Object>>map(preference -> Map.of("exists", true, "preference", preference))
                .orElseGet(() -> Map.of("exists", false));
    }

    @PostMapping("/preferences/{userId}")
    @RequireRole
    public UserPreference savePreference(@PathVariable Long userId,
                                         @RequestBody UserPreferenceRequest request) {
        return recommendationService.savePreference(userId, request);
    }

    @GetMapping("/for-you/{userId}")
    @RequireRole
    public ForYouRecommendationResponse forYou(@PathVariable Long userId) {
        return recommendationService.recommendForUser(userId);
    }
}
