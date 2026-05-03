package com.example.minsumgr.service;

import com.example.minsumgr.domain.Booking;
import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.repository.BookingRepository;
import com.example.minsumgr.repository.HomestayRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 客服AI回复建议服务
 * 支持function call来查询订单、余房等信息
 */
@Service
public class ChatAiService {

    private static final Logger logger = LoggerFactory.getLogger(ChatAiService.class);
    private final Optional<ChatModel> chatModel;
    private final BookingRepository bookingRepository;
    private final HomestayRepository homestayRepository;
    private final SystemSettingService systemSettingService;
    private final QueryToolsService queryToolsService;
    private final ObjectMapper objectMapper;

    // 可用的Function名称列表
    private static final Set<String> AVAILABLE_FUNCTIONS = Set.of(
            "queryUserBookings",
            "queryAllHomestays", 
            "queryHomestayDetail",
            "queryHomestayAvailability",
            "queryAllActivities",
            "queryAllRoutes",
            "queryBookingDetail",
            "searchHomestays",
            "createBookingDraft",
            "createMultipleBookingDrafts"
    );

    public ChatAiService(Optional<ChatModel> chatModel, BookingRepository bookingRepository,
                        HomestayRepository homestayRepository, SystemSettingService systemSettingService,
                        QueryToolsService queryToolsService) {
        this.chatModel = chatModel;
        this.bookingRepository = bookingRepository;
        this.homestayRepository = homestayRepository;
        this.systemSettingService = systemSettingService;
        this.queryToolsService = queryToolsService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 获取AI客服建议回复（支持Function Calling）
     * @param userId 用户ID
     * @param conversationContext 对话上下文
     * @return AI建议回复
     */
    public String getCustomerServiceReply(Long userId, String conversationContext) {
        try {
            // 获取用户的订单信息作为基础上下文
            List<Booking> userBookings = bookingRepository.findByUserId(userId);
            String bookingInfo = buildBookingInfo(userBookings);
            
            // 构建带Function Calling支持的提示词
            String systemPrompt = buildCustomerServiceSystemPrompt(userId, bookingInfo);
            
            Optional<ChatModel> resolvedChatModel = resolveChatModel();
            if (resolvedChatModel.isEmpty()) {
                logger.warn("AI服务未配置");
                return "AI服务未配置，请先在管理员面板配置AI参数";
            }
            
            try {
                // 创建带Function Calling的请求
                List<Message> messages = new ArrayList<>();
                messages.add(new SystemMessage(systemPrompt));
                messages.add(new UserMessage(conversationContext));
                
                // 构建Function Callbacks
                List<FunctionCallback> functionCallbacks = buildFunctionCallbacks(userId);
                
                // 创建带Functions的Options
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withFunctionCallbacks(functionCallbacks)
                        .build();
                
                Prompt aiPrompt = new Prompt(messages, options);
                ChatResponse response = resolvedChatModel.get().call(aiPrompt);
                String content = response.getResult().getOutput().getContent();
                logger.info("AI回复生成成功（支持Function Calling）");
                return content;
            } catch (Exception e) {
                logger.error("AI模型调用失败", e);
                // 回退到不带Function的普通调用
                return fallbackReply(userId, bookingInfo, conversationContext, resolvedChatModel.get());
            }
        } catch (Exception e) {
            logger.error("获取AI客服建议异常", e);
            return "获取AI建议失败: " + e.getMessage();
        }
    }

    /**
     * 回退的普通回复方式（不使用Function Calling）
     */
    private String fallbackReply(Long userId, String bookingInfo, String conversationContext, ChatModel model) {
        try {
            String prompt = buildCustomerServicePrompt(userId, bookingInfo, conversationContext);
            Prompt aiPrompt = new Prompt(prompt);
            ChatResponse response = model.call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            logger.info("AI回复生成成功（回退模式）");
            return content;
        } catch (Exception e) {
            logger.error("回退模式调用失败", e);
            return "AI生成失败: " + e.getMessage();
        }
    }

    /**
     * 构建Function Callbacks列表
     */
    private List<FunctionCallback> buildFunctionCallbacks(Long userId) {
        List<FunctionCallback> callbacks = new ArrayList<>();
        
        // 查询用户订单
        callbacks.add(FunctionCallbackWrapper.builder(new QueryUserBookingsFunction(queryToolsService, userId))
                .withName("queryUserBookings")
                .withDescription("查询当前用户的所有订单记录，包含订单ID、民宿名称、入住日期、退房日期、状态、价格等信息")
                .build());
        
        // 查询所有民宿
        callbacks.add(FunctionCallbackWrapper.builder(new QueryAllHomestaysFunction(queryToolsService))
                .withName("queryAllHomestays")
                .withDescription("查询所有民宿列表，返回民宿ID、名称、位置、描述、每晚价格、可用房间数等信息")
                .build());
        
        // 查询民宿详情
        callbacks.add(FunctionCallbackWrapper.builder(new QueryHomestayDetailFunction(queryToolsService))
                .withName("queryHomestayDetail")
                .withDescription("查询指定民宿的详细信息，包含名称、位置、描述、价格、可用房间数、房间注意事项等。参数：homestayId（民宿ID）")
                .build());
        
        // 查询民宿可用情况
        callbacks.add(FunctionCallbackWrapper.builder(new QueryHomestayAvailabilityFunction(queryToolsService))
                .withName("queryHomestayAvailability")
                .withDescription("查询民宿在指定日期范围内的可用情况，判断是否可以预订。参数：homestayId（民宿ID）、startDate（开始日期，格式yyyy-MM-dd）、endDate（结束日期，格式yyyy-MM-dd）")
                .build());
        
        // 查询所有活动
        callbacks.add(FunctionCallbackWrapper.builder(new QueryAllActivitiesFunction(queryToolsService))
                .withName("queryAllActivities")
                .withDescription("查询所有游玩活动项目列表，返回活动ID、名称、描述、地点等信息")
                .build());
        
        // 查询所有路线
        callbacks.add(FunctionCallbackWrapper.builder(new QueryAllRoutesFunction(queryToolsService))
                .withName("queryAllRoutes")
                .withDescription("查询所有旅游路线列表，返回路线ID、名称、描述、标签等信息")
                .build());
        
        // 查询订单详情
        callbacks.add(FunctionCallbackWrapper.builder(new QueryBookingDetailFunction(queryToolsService))
                .withName("queryBookingDetail")
                .withDescription("查询指定订单的详细信息，包含订单ID、用户ID、民宿信息、入住退房日期、状态、价格、客人数量、特殊要求等。参数：bookingId（订单ID）")
                .build());
        
        // 搜索民宿
        callbacks.add(FunctionCallbackWrapper.builder(new SearchHomestaysFunction(queryToolsService))
                .withName("searchHomestays")
                .withDescription("按关键词搜索民宿，在民宿名称、位置、描述中进行模糊匹配。参数：keyword（搜索关键词）")
                .build());
        
        // 创建订单草稿
        callbacks.add(FunctionCallbackWrapper.builder(new CreateBookingDraftFunction(queryToolsService, userId))
                .withName("createBookingDraft")
                .withDescription("为用户创建民宿预订草稿，生成待确认的订单方案。当用户表示想预订某个民宿时调用此函数。参数：homestayId（民宿ID）、checkInDate（入住日期，格式yyyy-MM-dd）、checkOutDate（退房日期，格式yyyy-MM-dd）、guestCount（入住人数，默认1）、recommendReason（推荐理由）")
                .build());
        
        // 批量创建订单草稿
        callbacks.add(FunctionCallbackWrapper.builder(new CreateMultipleBookingDraftsFunction(queryToolsService, userId))
                .withName("createMultipleBookingDrafts")
                .withDescription("批量创建多个民宿预订草稿，为用户提供多个可选方案。当需要推荐多个民宿供用户选择时使用。参数：draftsJson（订单草稿数组的JSON字符串，每个元素包含homestayId、checkInDate、checkOutDate、guestCount、recommendReason）")
                .build());
        
        return callbacks;
    }

    // ========== Function实现内部类 ==========

    public record QueryUserBookingsRequest() {}
    public static class QueryUserBookingsFunction implements java.util.function.Function<QueryUserBookingsRequest, String> {
        private final QueryToolsService service;
        private final Long userId;
        public QueryUserBookingsFunction(QueryToolsService service, Long userId) {
            this.service = service;
            this.userId = userId;
        }
        @Override
        public String apply(QueryUserBookingsRequest req) {
            return service.queryUserBookings(userId);
        }
    }

    public record EmptyRequest() {}
    public static class QueryAllHomestaysFunction implements java.util.function.Function<EmptyRequest, String> {
        private final QueryToolsService service;
        public QueryAllHomestaysFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(EmptyRequest req) { return service.queryAllHomestays(); }
    }

    public record HomestayIdRequest(Long homestayId) {}
    public static class QueryHomestayDetailFunction implements java.util.function.Function<HomestayIdRequest, String> {
        private final QueryToolsService service;
        public QueryHomestayDetailFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(HomestayIdRequest req) { return service.queryHomestayDetail(req.homestayId()); }
    }

    public record AvailabilityRequest(Long homestayId, String startDate, String endDate) {}
    public static class QueryHomestayAvailabilityFunction implements java.util.function.Function<AvailabilityRequest, String> {
        private final QueryToolsService service;
        public QueryHomestayAvailabilityFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(AvailabilityRequest req) {
            return service.queryHomestayAvailability(req.homestayId(), req.startDate(), req.endDate());
        }
    }

    public static class QueryAllActivitiesFunction implements java.util.function.Function<EmptyRequest, String> {
        private final QueryToolsService service;
        public QueryAllActivitiesFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(EmptyRequest req) { return service.queryAllActivities(); }
    }

    public static class QueryAllRoutesFunction implements java.util.function.Function<EmptyRequest, String> {
        private final QueryToolsService service;
        public QueryAllRoutesFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(EmptyRequest req) { return service.queryAllRoutes(); }
    }

    public record BookingIdRequest(Long bookingId) {}
    public static class QueryBookingDetailFunction implements java.util.function.Function<BookingIdRequest, String> {
        private final QueryToolsService service;
        public QueryBookingDetailFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(BookingIdRequest req) { return service.queryBookingDetail(req.bookingId()); }
    }

    public record KeywordRequest(String keyword) {}
    public static class SearchHomestaysFunction implements java.util.function.Function<KeywordRequest, String> {
        private final QueryToolsService service;
        public SearchHomestaysFunction(QueryToolsService service) { this.service = service; }
        @Override
        public String apply(KeywordRequest req) { return service.searchHomestays(req.keyword()); }
    }

    // ========== 创建订单草稿 Function ==========
    public record CreateBookingDraftRequest(Long homestayId, String checkInDate, String checkOutDate, 
                                             Integer guestCount, String recommendReason) {}
    public static class CreateBookingDraftFunction implements java.util.function.Function<CreateBookingDraftRequest, String> {
        private final QueryToolsService service;
        private final Long userId;
        public CreateBookingDraftFunction(QueryToolsService service, Long userId) {
            this.service = service;
            this.userId = userId;
        }
        @Override
        public String apply(CreateBookingDraftRequest req) {
            return service.createBookingDraft(userId, req.homestayId(), req.checkInDate(), 
                    req.checkOutDate(), req.guestCount(), req.recommendReason());
        }
    }

    // ========== 批量创建订单草稿 Function ==========
    public record CreateMultipleBookingDraftsRequest(String draftsJson) {}
    public static class CreateMultipleBookingDraftsFunction implements java.util.function.Function<CreateMultipleBookingDraftsRequest, String> {
        private final QueryToolsService service;
        private final Long userId;
        public CreateMultipleBookingDraftsFunction(QueryToolsService service, Long userId) {
            this.service = service;
            this.userId = userId;
        }
        @Override
        public String apply(CreateMultipleBookingDraftsRequest req) {
            return service.createMultipleBookingDrafts(userId, req.draftsJson());
        }
    }

    /**
     * 查询用户订单信息
     * @param userId 用户ID
     * @return 订单信息JSON
     */
    public String queryUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserId(userId);
        
        if (bookings.isEmpty()) {
            return "用户暂无订单";
        }
        
        List<Map<String, Object>> bookingList = bookings.stream()
                .map(booking -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("orderId", booking.getId());
                    map.put("homestayId", booking.getHomestayId());
                    map.put("checkIn", booking.getCheckInDate());
                    map.put("checkOut", booking.getCheckOutDate());
                    map.put("status", booking.getStatus());
                    map.put("totalPrice", booking.getTotalPrice());
                    
                    // 获取民宿名称
                    Homestay homestay = homestayRepository.findById(booking.getHomestayId()).orElse(null);
                    if (homestay != null) {
                        map.put("homestayName", homestay.getName());
                    }
                    
                    return map;
                })
                .collect(Collectors.toList());
        
        try {
            return objectMapper.writeValueAsString(bookingList);
        } catch (Exception e) {
            return "订单查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询民宿可用房间数
     * @param homestayId 民宿ID
     * @return 房间信息
     */
    public String queryAvailableRooms(Long homestayId) {
        Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
        
        if (homestay == null) {
            return "民宿不存在";
        }
        
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("homestayId", homestay.getId());
        info.put("name", homestay.getName());
        info.put("location", homestay.getLocation());
        info.put("availableRooms", homestay.getAvailableRooms());
        info.put("pricePerNight", homestay.getPricePerNight());
        info.put("description", homestay.getDescription());
        info.put("roomNotes", homestay.getRoomNotes());
        
        try {
            return objectMapper.writeValueAsString(info);
        } catch (Exception e) {
            return "房间查询失败: " + e.getMessage();
        }
    }

    /**
     * 查询日期范围内的预订情况
     * @param homestayId 民宿ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 预订情况
     */
    public String queryBookingsByDateRange(Long homestayId, String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            // 查询该民宿该日期范围内的所有预订
            List<Booking> bookings = bookingRepository.findByHomestayIdAndStatus(homestayId, "CONFIRMED");
            
            // 过滤日期范围
            List<Booking> relevantBookings = bookings.stream()
                    .filter(b -> !b.getCheckOutDate().isBefore(start) && !b.getCheckInDate().isAfter(end))
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("homestayId", homestayId);
            result.put("dateRange", startDate + " 至 " + endDate);
            result.put("totalBookings", relevantBookings.size());
            result.put("bookings", relevantBookings.stream()
                    .map(b -> new LinkedHashMap<String, Object>() {{
                        put("orderId", b.getId());
                        put("checkIn", b.getCheckInDate());
                        put("checkOut", b.getCheckOutDate());
                        put("status", b.getStatus());
                    }})
                    .collect(Collectors.toList()));
            
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            return "日期范围查询失败: " + e.getMessage();
        }
    }

    /**
     * 构建用户订单信息摘要
     */
    private String buildBookingInfo(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return "用户暂无订单记录";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("## 用户订单记录\n");
        for (Booking booking : bookings) {
            Homestay homestay = homestayRepository.findById(booking.getHomestayId()).orElse(null);
            sb.append(String.format("- 订单#%d: %s (入住: %s, 退房: %s, 状态: %s, 金额: ¥%s)\n",
                    booking.getId(),
                    homestay != null ? homestay.getName() : "民宿#" + booking.getHomestayId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getStatus(),
                    booking.getTotalPrice()));
        }
        return sb.toString();
    }

    /**
     * 构建支持Function Calling的系统提示词
     */
    private String buildCustomerServiceSystemPrompt(Long userId, String bookingInfo) {
        return """
                你是一个AI写作助手，帮助客服人员撰写给客户的回复。你需要以客服的身份（第一人称「我」）直接生成发给客户的消息。
                
                ## 重要说明
                - 你生成的内容将直接作为客服发送给客户的消息
                - 请使用第一人称「我」来代表客服
                - 称呼客户时使用「您」
                - 语气要专业、热情、亲切
                
                ## 客服可处理的业务
                - 帮助客户解决订单和预订相关问题
                - 提供民宿信息咨询
                - 处理取消、修改预订等请求
                - 推荐合适的民宿、活动和旅游路线
                - **帮助客户生成民宿预订订单草稿**
                
                ## 当前客户信息
                客户ID: %d
                %s
                
                ## 可用的查询工具
                你可以使用以下工具来获取最新数据，以便给客户提供准确信息：
                1. queryUserBookings - 查询该客户的所有订单
                2. queryAllHomestays - 获取所有可用民宿列表
                3. queryHomestayDetail - 查询指定民宿的详细信息
                4. queryHomestayAvailability - 查询民宿在某日期段的可用情况
                5. queryAllActivities - 获取所有游玩活动列表
                6. queryAllRoutes - 获取所有旅游路线列表
                7. queryBookingDetail - 查询指定订单的详情
                8. searchHomestays - 按关键词搜索民宿
                9. **createBookingDraft** - 为客户创建单个民宿预订草稿（当客户明确想预订某个民宿时使用）
                10. **createMultipleBookingDrafts** - 批量创建多个预订草稿供客户选择（当推荐多个民宿方案时使用）
                
                ## 预订订单生成说明
                当客户表达预订意向时：
                1. 如果客户明确指定了民宿，使用 createBookingDraft 生成单个订单草稿
                2. 如果需要推荐多个民宿供选择，使用 createMultipleBookingDrafts 生成多个方案
                3. 生成草稿后，系统会返回订单信息，你需要向客户展示并请求确认
                4. 草稿中包含价格、日期、房间等信息，请清晰告知客户
                
                ## 回复格式要求
                1. 直接输出发给客户的消息内容，不要有任何前缀或说明
                2. 语言专业友好，体现高质量服务
                3. 根据查询结果给出准确的信息
                4. 给出清晰的解决方案或下一步步骤
                5. 可以适度使用emoji增加亲和力
                6. 回复控制在200字以内
                7. 如果生成了订单草稿，请在回复中包含关键信息（民宿名称、日期、价格等）并提示客户确认
                
                ## 如果对话中包含【客服指导要求】
                请严格按照客服的指导方向来生成回复，这是客服希望回复的重点和方向。
                """.formatted(userId, bookingInfo);
    }

    /**
     * 构建客服AI提示词（回退模式使用）
     */
    private String buildCustomerServicePrompt(Long userId, String bookingInfo, String conversationContext) {
        return """
                你是一个AI写作助手，帮助客服人员撰写给客户的回复。请根据对话内容，以客服的身份（第一人称「我」）生成发给客户的消息。
                
                ## 重要说明
                - 你生成的内容将直接作为客服发送给客户的消息
                - 请使用第一人称「我」来代表客服
                - 称呼客户时使用「您」
                - 不要输出任何前缀、说明或引号，直接输出消息内容
                
                ## 客服可处理的业务
                - 帮助客户解决订单和预订相关问题
                - 提供民宿信息咨询
                - 处理取消、修改预订等请求
                
                ## 客户信息
                客户ID: %d
                %s
                
                ## 对话记录
                %s
                
                ## 回复格式要求
                1. 直接输出发给客户的消息，不要有「建议回复：」等前缀
                2. 语言专业友好，体现高质量服务
                3. 直接解决客户问题
                4. 给出清晰的解决方案或下一步步骤
                5. 可以适度使用emoji增加亲和力
                6. 回复控制在200字以内
                
                ## 如果对话中包含【客服指导要求】
                这是客服希望你遵循的回复方向和重点，请严格按照指导来生成回复内容。
                """.formatted(userId, bookingInfo, conversationContext);
    }

    /**
     * 解析ChatModel（支持自定义配置或环境变量）
     */
    private Optional<ChatModel> resolveChatModel() {
        try {
            var setting = systemSettingService.getSystemSetting();
            Boolean enabled = setting.getAiEnabled();
            
            logger.info("AI服务配置状态: enabled={}", enabled);
            
            if (enabled != null && enabled) {
                String apiKey = setting.getAiApiKey();
                if (apiKey == null || apiKey.isBlank()) {
                    logger.warn("AI API密钥未配置");
                    return Optional.empty();
                }
                
                String baseUrl = setting.getAiBaseUrl();
                String model = setting.getAiModel();
                
                logger.info("读取到AI配置: baseUrl={}, model={}", baseUrl, model);
                
                OpenAiApi api;
                if (baseUrl != null && !baseUrl.isBlank()) {
                    logger.info("使用自定义API地址: {}", baseUrl);
                    api = new OpenAiApi(baseUrl, apiKey);
                } else {
                    logger.info("使用默认API地址");
                    api = new OpenAiApi(apiKey);
                }
                
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withModel(model != null && !model.isBlank() ? model : "gpt-3.5-turbo")
                        .build();
                
                logger.info("创建OpenAiChatModel，模型: {}", options.getModel());
                return Optional.of(new OpenAiChatModel(api, options));
            }
            
            logger.info("AI服务未启用，使用默认ChatModel");
            return chatModel;
        } catch (Exception e) {
            logger.error("解析ChatModel失败", e);
            // Return empty instead of throwing to allow graceful degradation
            return Optional.empty();
        }
    }
}
