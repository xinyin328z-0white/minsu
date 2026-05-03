package com.example.minsumgr.service;

import com.example.minsumgr.domain.Booking;
import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.domain.Activity;
import com.example.minsumgr.domain.Route;
import com.example.minsumgr.dto.TravelRecommendationRequest;
import com.example.minsumgr.dto.TravelRecommendationResponse;
import com.example.minsumgr.repository.BookingRepository;
import com.example.minsumgr.repository.HomestayRepository;
import com.example.minsumgr.repository.ActivityRepository;
import com.example.minsumgr.repository.RouteRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AiService {

    private final Optional<ChatModel> chatModel;
    private final BookingRepository bookingRepository;
    private final HomestayRepository homestayRepository;
    private final ActivityRepository activityRepository;
    private final RouteRepository routeRepository;
    private final SystemSettingService systemSettingService;

    public AiService(Optional<ChatModel> chatModel, BookingRepository bookingRepository,
                     HomestayRepository homestayRepository, ActivityRepository activityRepository,
                     RouteRepository routeRepository, SystemSettingService systemSettingService) {
        this.chatModel = chatModel;
        this.bookingRepository = bookingRepository;
        this.homestayRepository = homestayRepository;
        this.activityRepository = activityRepository;
        this.routeRepository = routeRepository;
        this.systemSettingService = systemSettingService;
    }

    public String recommendForUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        String orders = bookings.stream()
                .map(b -> String.format("订单#%d: 民宿=%d, 入住=%s, 退房=%s, 状态=%s, 金额=%s",
                        b.getId(), b.getHomestayId(), b.getCheckInDate(), b.getCheckOutDate(), b.getStatus(), b.getTotalPrice()))
                .collect(Collectors.joining("\n"));

        String promptText = "你是民宿推荐与路线规划助手。\n\n" +
                "## 用户历史订单\n" +
                (orders.isBlank() ? "该用户暂无历史订单" : orders) + "\n\n" +
                "## 任务\n" +
                "请根据用户的历史订单特点，给出**3条民宿推荐理由**和**2条旅游路线建议**。\n\n" +
                "## 输出格式\n" +
                "请使用Markdown格式，包括清晰的标题和项目符号。\n\n" +
                "## 推荐思路\n" +
                "- 分析用户偏好（如价格范围、位置等）\n" +
                "- 基于历史选择提供相似或升级的选项\n" +
                "- 提供详细的推荐理由";

        Optional<ChatModel> resolvedChatModel = resolveChatModel();
        if (resolvedChatModel.isEmpty()) {
            return "演示模式：AI推荐服务未配置或已关闭。";
        }

        try {
            Prompt prompt = new Prompt(promptText);
            ChatResponse response = resolvedChatModel.get().call(prompt);
            return response.getResult().getOutput().getContent();
        } catch (Exception e) {
            return "演示模式：AI服务调用失败，请检查配置。错误：" + e.getMessage();
        }
    }

    /**
     * 生成旅行计划和民宿推荐
     */
    public TravelRecommendationResponse generateTravelRecommendation(TravelRecommendationRequest request) {
        // 获取所有必需的数据
        List<Homestay> homestays = homestayRepository.findAll();
        List<Activity> activities = activityRepository.findAll();
        List<Route> routes = routeRepository.findAll();
        
        // 构建数据摘要
        LocalDate startDate = LocalDate.now().plusDays(request.getStartDate() != null ? request.getStartDate() : 0);
        LocalDate endDate = startDate.plusDays(request.getDays() != null ? request.getDays() : 3);
        
        String homestayInfo = buildHomestayInfo(homestays, startDate, endDate);
        String activityInfo = buildActivityInfo(activities);
        String routeInfo = buildRouteInfo(routes);
        
        // 构建完整提示词
        String prompt = buildTravelPrompt(request, homestayInfo, activityInfo, routeInfo);
        
        TravelRecommendationResponse response = new TravelRecommendationResponse();
        
        Optional<ChatModel> resolvedChatModel = resolveChatModel();
        if (resolvedChatModel.isEmpty()) {
            response.setRecommendation("演示模式：AI服务未配置或已关闭");
            response.setHomestayAdvice("请根据上述民宿信息和您的偏好选择");
            response.setItinerary("请根据上述旅游项目和路线规划您的行程");
            return response;
        }

        try {
            Prompt aiPrompt = new Prompt(prompt);
            ChatResponse chatResponse = resolvedChatModel.get().call(aiPrompt);
            String result = chatResponse.getResult().getOutput().getContent();
            
            // 解析结果并填充响应
            response.setRecommendation(result);
            response.setHomestayAdvice(extractSection(result, "民宿建议"));
            response.setItinerary(extractSection(result, "行程规划"));
            
            return response;
        } catch (Exception e) {
            response.setRecommendation("演示模式：AI服务调用失败，请检查配置。错误：" + e.getMessage());
            return response;
        }
    }

    private String buildHomestayInfo(List<Homestay> homestays, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder("可用民宿信息：\n");
        for (Homestay h : homestays) {
            String price = h.getPricePerNight() != null ? h.getPricePerNight().toPlainString() : "未知";
            sb.append(String.format(
                "- %s (位置: %s, 价格: ¥%s/晚, 房间数: %d, 备注: %s)\n",
                h.getName(), h.getLocation(), price, h.getAvailableRooms(), 
                h.getRoomNotes() != null ? h.getRoomNotes() : "无特殊说明"
            ));
        }
        return sb.toString();
    }

    private String buildActivityInfo(List<Activity> activities) {
        StringBuilder sb = new StringBuilder("游玩项目：\n");
        for (Activity a : activities) {
            sb.append(String.format(
                "- %s (地点: %s, 描述: %s)\n",
                a.getName(), a.getLocation(), a.getDescription() != null ? a.getDescription() : "无描述"
            ));
        }
        return sb.toString();
    }

    private String buildRouteInfo(List<Route> routes) {
        StringBuilder sb = new StringBuilder("旅游路线：\n");
        for (Route r : routes) {
            sb.append(String.format(
                "- %s (标签: %s, 描述: %s)\n",
                r.getName(), r.getTags() != null ? r.getTags() : "无标签", 
                r.getDescription() != null ? r.getDescription() : "无描述"
            ));
        }
        return sb.toString();
    }

    private String buildTravelPrompt(TravelRecommendationRequest request, String homestayInfo, 
                                     String activityInfo, String routeInfo) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一位专业的旅游规划师和民宿推荐专家。\n\n");
        
        prompt.append("## 用户信息\n");
        if (request.getDays() != null) {
            prompt.append("- **旅行天数**：").append(request.getDays()).append("天\n");
        }
        if (request.getStartDate() != null) {
            LocalDate start = LocalDate.now().plusDays(request.getStartDate());
            prompt.append("- **计划开始日期**：").append(start).append("\n");
        }
        
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isEmpty()) {
            prompt.append("- **特殊要求**：").append(request.getCustomPrompt()).append("\n\n");
        } else {
            prompt.append("\n");
        }
        
        prompt.append("## 可用资源信息\n");
        prompt.append(homestayInfo).append("\n");
        prompt.append(activityInfo).append("\n");
        prompt.append(routeInfo).append("\n");
        
        prompt.append("## 任务要求\n");
        prompt.append("请基于上述信息，为用户提供详细的旅行方案，包括：\n\n");
        
        prompt.append("### 1. 民宿建议\n");
        prompt.append("- 根据旅行天数和用户需求推荐2-3个最合适的民宿\n");
        prompt.append("- 详细说明每个民宿的选择理由\n");
        prompt.append("- 考虑地理位置、价格、设施等因素\n\n");
        
        prompt.append("### 2. 行程规划\n");
        prompt.append("- 设计一个详细的每日行程安排\n");
        prompt.append("- 包括上午、下午、晚上的活动\n");
        prompt.append("- 合理分配各个游玩项目和旅游路线\n");
        prompt.append("- 考虑交通和休息时间\n\n");
        
        prompt.append("### 3. 旅行建议\n");
        prompt.append("- 提供实用的旅行贴士（如最佳出行时间、推荐季节等）\n");
        prompt.append("- 给出安全提示和注意事项\n");
        prompt.append("- 推荐当地特色美食或体验\n\n");
        
        prompt.append("请用**清晰的Markdown格式**详细回答，确保建议具有可操作性。");
        
        return prompt.toString();
    }

    private String extractSection(String content, String sectionName) {
        // 简单的章节提取逻辑
        int startIdx = content.indexOf(sectionName);
        if (startIdx == -1) {
            return content;
        }
        return content.substring(startIdx);
    }

    private Optional<ChatModel> resolveChatModel() {
        var setting = systemSettingService.getSystemSetting();
        Boolean enabled = setting.getAiEnabled();
        if (enabled != null && enabled) {
            String apiKey = setting.getAiApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                return Optional.empty();
            }
            String baseUrl = setting.getAiBaseUrl();
            String model = setting.getAiModel();
            
            // 创建OpenAiApi实例
            OpenAiApi api;
            if (baseUrl != null && !baseUrl.isBlank()) {
                api = new OpenAiApi(baseUrl, apiKey);
            } else {
                api = new OpenAiApi(apiKey);
            }
            
            // 创建ChatOptions并设置模型
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withModel(model != null && !model.isBlank() ? model : "gpt-3.5-turbo")
                    .build();
            
            return Optional.of(new OpenAiChatModel(api, options));
        }
        return chatModel;
    }
}
