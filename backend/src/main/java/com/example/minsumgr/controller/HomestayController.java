package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.dto.BookingDraftRequest;
import com.example.minsumgr.dto.BookingDraftResponse;
import com.example.minsumgr.dto.TravelRecommendationRequest;
import com.example.minsumgr.dto.TravelRecommendationResponse;
import com.example.minsumgr.repository.HomestayRepository;
import com.example.minsumgr.service.AiService;
import com.example.minsumgr.service.AiBookingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/homestays")
@CrossOrigin(origins = "http://localhost:5173")
public class HomestayController {

    private final HomestayRepository repository;
    private final AiService aiService;
    private final AiBookingService aiBookingService;

    public HomestayController(HomestayRepository repository, AiService aiService, 
                             AiBookingService aiBookingService) {
        this.repository = repository;
        this.aiService = aiService;
        this.aiBookingService = aiBookingService;
    }

    @GetMapping
    public List<Homestay> list() {
        return repository.findAll();
    }

    @PostMapping
    public Homestay create(@RequestBody Homestay homestay) {
        return repository.save(homestay);
    }

    @PutMapping("/{id}")
    public Homestay update(@PathVariable Long id, @RequestBody Homestay homestay) {
        homestay.setId(id);
        return repository.save(homestay);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    @PostMapping("/ai-recommend")
    @RequireRole
    public TravelRecommendationResponse getAiRecommendation(@RequestBody TravelRecommendationRequest request) {
        return aiService.generateTravelRecommendation(request);
    }

    /**
     * AI生成订单草稿
     * 根据用户需求生成一个或多个民宿预订方案供用户选择
     */
    @PostMapping("/ai-booking-draft")
    @RequireRole
    public BookingDraftResponse generateBookingDraft(@RequestBody BookingDraftRequest request) {
        return aiBookingService.generateBookingDraft(request);
    }
}
