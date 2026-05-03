package com.example.minsumgr.service;

import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.dto.BookingDraftRequest;
import com.example.minsumgr.dto.BookingDraftResponse;
import com.example.minsumgr.repository.HomestayRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI预订订单生成服务
 */
@Service
public class AiBookingService {

    private static final Logger logger = LoggerFactory.getLogger(AiBookingService.class);
    
    private final Optional<ChatModel> chatModel;
    private final HomestayRepository homestayRepository;
    private final SystemSettingService systemSettingService;
    private final QueryToolsService queryToolsService;
    private final ObjectMapper objectMapper;
    
    // 用于存储批量订单草稿结果
    private volatile String lastBookingDraftsResult = null;

    public AiBookingService(Optional<ChatModel> chatModel, 
                           HomestayRepository homestayRepository,
                           SystemSettingService systemSettingService,
                           QueryToolsService queryToolsService) {
        this.chatModel = chatModel;
        this.homestayRepository = homestayRepository;
        this.systemSettingService = systemSettingService;
        this.queryToolsService = queryToolsService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 生成预订草稿
     */
    public BookingDraftResponse generateBookingDraft(BookingDraftRequest request) {
        logger.info("开始生成AI订单草稿: userId={}, days={}", request.getUserId(), request.getDays());
        
        // 清空之前的结果
        lastBookingDraftsResult = null;
        
        try {
            Optional<ChatModel> resolvedChatModel = resolveChatModel();
            if (resolvedChatModel.isEmpty()) {
                return createFallbackResponse("AI服务未配置或已关闭，无法生成智能订单推荐。");
            }

            // 构建提示词
            String systemPrompt = buildBookingPrompt(request);
            String userPrompt = buildUserPrompt(request);
            
            // 构建Function Callbacks
            List<FunctionCallback> functionCallbacks = buildFunctionCallbacks(request.getUserId());
            
            // 创建带Functions的Options
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withFunctionCallbacks(functionCallbacks)
                    .build();
            
            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(systemPrompt));
            messages.add(new UserMessage(userPrompt));
            
            Prompt aiPrompt = new Prompt(messages, options);
            ChatResponse response = resolvedChatModel.get().call(aiPrompt);
            String content = response.getResult().getOutput().getContent();
            
            logger.info("AI生成订单草稿成功: {}", content);
            
            // 从Response中提取函数调用结果
            return parseAiResponse(response, content, request);
            
        } catch (Exception e) {
            logger.error("生成AI订单草稿失败", e);
            return createFallbackResponse("生成订单草稿失败: " + e.getMessage());
        }
    }

    private String buildBookingPrompt(BookingDraftRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("你是一个专业的旅行预订助手，专门帮助用户生成民宿预订方案。\n\n");
        
        prompt.append("## 核心任务（必须执行）\n");
        prompt.append("你必须完成以下步骤来生成订单草稿：\n");
        prompt.append("1. 调用 queryAllHomestays() 获取民宿列表\n");
        prompt.append("2. 筛选2-3个最适合的民宿\n");
        prompt.append("3. **必须调用 createMultipleBookingDrafts() 生成订单草稿**（这是最重要的步骤）\n\n");
        
        prompt.append("## createMultipleBookingDrafts 参数格式\n");
        prompt.append("必须传递一个JSON数组字符串，格式如下：\n");
        prompt.append("```json\n");
        prompt.append("[\n");
        prompt.append("  {\n");
        prompt.append("    \"homestayId\": 1,\n");
        prompt.append("    \"checkInDate\": \"2026-02-02\",\n");
        prompt.append("    \"checkOutDate\": \"2026-02-05\",\n");
        prompt.append("    \"guestCount\": 2,\n");
        prompt.append("    \"recommendReason\": \"推荐理由\"\n");
        prompt.append("  }\n");
        prompt.append("]\n");
        prompt.append("```\n\n");
        
        prompt.append("## 关键要求\n");
        prompt.append("1. **务必调用 createMultipleBookingDrafts 函数**，否则前端无法显示订单\n");
        prompt.append("2. 日期必须使用 yyyy-MM-dd 格式\n");
        prompt.append("3. 推荐2-3个民宿方案\n");
        prompt.append("4. 每个方案都要有清晰的推荐理由\n\n");
        
        prompt.append("## 回复格式\n");
        prompt.append("在调用函数生成草稿后，用简短的话告诉用户：\n");
        prompt.append("\"已为您生成X个预订方案，请在对话框中选择您心仪的民宿。\"\n");
        
        return prompt.toString();
    }

    private String buildUserPrompt(BookingDraftRequest request) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("用户需求信息：\n");
        if (request.getDays() != null) {
            prompt.append("- 计划住宿天数：").append(request.getDays()).append("天\n");
        }
        if (request.getStartDate() != null) {
            LocalDate startDate = LocalDate.now().plusDays(request.getStartDate());
            LocalDate endDate = startDate.plusDays(request.getDays() != null ? request.getDays() : 3);
            prompt.append("- 入住日期：").append(startDate).append("\n");
            prompt.append("- 退房日期：").append(endDate).append("\n");
        }
        if (request.getBudget() != null && !request.getBudget().isEmpty()) {
            prompt.append("- 预算偏好：").append(request.getBudget()).append("\n");
        }
        if (request.getCustomPrompt() != null && !request.getCustomPrompt().isEmpty()) {
            prompt.append("- 特殊要求：").append(request.getCustomPrompt()).append("\n");
        }
        
        prompt.append("\n请为用户生成合适的民宿预订方案。");
        
        return prompt.toString();
    }

    private List<FunctionCallback> buildFunctionCallbacks(Long userId) {
        List<FunctionCallback> callbacks = new ArrayList<>();
        
        // 这里复用 ChatAiService 中的 Function 实现
        callbacks.add(FunctionCallbackWrapper.builder(new QueryAllHomestaysFunction(queryToolsService))
                .withName("queryAllHomestays")
                .withDescription("获取所有可用民宿列表")
                .build());
        
        callbacks.add(FunctionCallbackWrapper.builder(new QueryHomestayDetailFunction(queryToolsService))
                .withName("queryHomestayDetail")
                .withDescription("查询指定民宿的详细信息")
                .build());
        
        callbacks.add(FunctionCallbackWrapper.builder(new QueryHomestayAvailabilityFunction(queryToolsService))
                .withName("queryHomestayAvailability")
                .withDescription("查询民宿在指定日期范围内的可用情况")
                .build());
        
        callbacks.add(FunctionCallbackWrapper.builder(new CreateBookingDraftFunction(queryToolsService, userId))
                .withName("createBookingDraft")
                .withDescription("为用户创建单个民宿预订草稿")
                .build());
        
        callbacks.add(FunctionCallbackWrapper.builder(new CreateMultipleBookingDraftsFunction(queryToolsService, userId))
                .withName("createMultipleBookingDrafts")
                .withDescription("批量创建多个预订草稿供用户选择")
                .build());
        
        return callbacks;
    }

    // Function 实现类（复用 ChatAiService 中的定义）
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

    public record CreateBookingDraftRequest(Long homestayId, String checkInDate, String checkOutDate, 
                                            Integer guestCount, String recommendReason) {}
    public class CreateBookingDraftFunction implements java.util.function.Function<CreateBookingDraftRequest, String> {
        private final QueryToolsService service;
        private final Long userId;
        public CreateBookingDraftFunction(QueryToolsService service, Long userId) {
            this.service = service;
            this.userId = userId;
        }
        @Override
        public String apply(CreateBookingDraftRequest req) {
            String result = service.createBookingDraft(userId, req.homestayId(), req.checkInDate(), 
                    req.checkOutDate(), req.guestCount(), req.recommendReason());
            // 将单个结果包装成数组格式保存
            try {
                Map<String, Object> singleDraft = objectMapper.readValue(result, Map.class);
                Map<String, Object> wrappedResult = new LinkedHashMap<>();
                wrappedResult.put("success", true);
                wrappedResult.put("userId", userId);
                wrappedResult.put("totalOptions", 1);
                wrappedResult.put("options", Arrays.asList(singleDraft));
                wrappedResult.put("message", "已为您生成1个预订方案，请确认订单信息");
                lastBookingDraftsResult = objectMapper.writeValueAsString(wrappedResult);
                logger.info("函数调用完成，保存单个草稿结果: {}", lastBookingDraftsResult);
            } catch (Exception e) {
                logger.error("包装单个草稿结果失败", e);
            }
            return result;
        }
    }

    public record CreateMultipleBookingDraftsRequest(String draftsJson) {}
    public class CreateMultipleBookingDraftsFunction implements java.util.function.Function<CreateMultipleBookingDraftsRequest, String> {
        private final QueryToolsService service;
        private final Long userId;
        public CreateMultipleBookingDraftsFunction(QueryToolsService service, Long userId) {
            this.service = service;
            this.userId = userId;
        }
        @Override
        public String apply(CreateMultipleBookingDraftsRequest req) {
            String result = service.createMultipleBookingDrafts(userId, req.draftsJson());
            // 保存结果供后续使用
            lastBookingDraftsResult = result;
            logger.info("函数调用完成，保存结果: {}", result);
            return result;
        }
    }

    private BookingDraftResponse parseAiResponse(ChatResponse response, String content, BookingDraftRequest request) {
        BookingDraftResponse draftResponse = new BookingDraftResponse();
        draftResponse.setSuccess(true);
        draftResponse.setAiExplanation(content);
        
        // 从保存的函数调用结果中提取订单草稿
        List<BookingDraftResponse.BookingOption> options = new ArrayList<>();
        
        try {
            if (lastBookingDraftsResult != null && !lastBookingDraftsResult.isBlank()) {
                logger.info("从函数调用结果中提取订单草稿: {}", lastBookingDraftsResult);
                
                Map<String, Object> resultMap = objectMapper.readValue(lastBookingDraftsResult, Map.class);
                List<Map<String, Object>> optionsList = (List<Map<String, Object>>) resultMap.get("options");
                
                if (optionsList != null && !optionsList.isEmpty()) {
                    options = parseOptionsFromJson(optionsList);
                    draftResponse.setMessage("AI已为您生成" + options.size() + "个预订方案，请选择您心仪的民宿");
                    logger.info("成功解析了{}个订单草稿", options.size());
                } else {
                    logger.warn("函数调用结果中没有options列表");
                    draftResponse.setMessage("AI已分析您的需求，但未能生成具体的订单草稿");
                }
            } else {
                logger.warn("没有找到函数调用结果");
                draftResponse.setMessage("AI已分析您的需求，但可能没有调用订单生成函数");
            }
            
        } catch (Exception e) {
            logger.error("解析AI响应失败", e);
            draftResponse.setMessage("解析订单草稿失败，请重试");
        }
        
        draftResponse.setOptions(options);
        return draftResponse;
    }
    
    /**
     * 从JSON列表解析订单选项
     */
    private List<BookingDraftResponse.BookingOption> parseOptionsFromJson(List<Map<String, Object>> optionsList) {
        List<BookingDraftResponse.BookingOption> options = new ArrayList<>();
        
        for (Map<String, Object> optionMap : optionsList) {
            BookingDraftResponse.BookingOption option = new BookingDraftResponse.BookingOption();
            
            if (optionMap.get("optionId") != null) {
                option.setOptionId(optionMap.get("optionId").toString());
            }
            if (optionMap.get("homestayId") != null) {
                option.setHomestayId(Long.valueOf(optionMap.get("homestayId").toString()));
            }
            if (optionMap.get("homestayName") != null) {
                option.setHomestayName(optionMap.get("homestayName").toString());
            }
            if (optionMap.get("homestayLocation") != null) {
                option.setHomestayLocation(optionMap.get("homestayLocation").toString());
            }
            if (optionMap.get("homestayImageUrl") != null) {
                option.setHomestayImageUrl(optionMap.get("homestayImageUrl").toString());
            }
            if (optionMap.get("checkInDate") != null) {
                option.setCheckInDate(LocalDate.parse(optionMap.get("checkInDate").toString()));
            }
            if (optionMap.get("checkOutDate") != null) {
                option.setCheckOutDate(LocalDate.parse(optionMap.get("checkOutDate").toString()));
            }
            if (optionMap.get("nights") != null) {
                option.setNights(Integer.valueOf(optionMap.get("nights").toString()));
            }
            if (optionMap.get("pricePerNight") != null) {
                option.setPricePerNight(new BigDecimal(optionMap.get("pricePerNight").toString()));
            }
            if (optionMap.get("totalPrice") != null) {
                option.setTotalPrice(new BigDecimal(optionMap.get("totalPrice").toString()));
            }
            if (optionMap.get("guestCount") != null) {
                option.setGuestCount(Integer.valueOf(optionMap.get("guestCount").toString()));
            }
            if (optionMap.get("isAvailable") != null) {
                option.setIsAvailable(Boolean.valueOf(optionMap.get("isAvailable").toString()));
            }
            if (optionMap.get("roomNotes") != null) {
                option.setRoomNotes(optionMap.get("roomNotes").toString());
            }
            if (optionMap.get("recommendReason") != null) {
                option.setRecommendReason(optionMap.get("recommendReason").toString());
            }
            
            options.add(option);
        }
        
        return options;
    }

    private BookingDraftResponse createFallbackResponse(String message) {
        BookingDraftResponse response = new BookingDraftResponse();
        response.setSuccess(false);
        response.setMessage(message);
        response.setOptions(new ArrayList<>());
        return response;
    }

    private Optional<ChatModel> resolveChatModel() {
        try {
            var setting = systemSettingService.getSystemSetting();
            Boolean enabled = setting.getAiEnabled();
            
            if (enabled != null && enabled) {
                String apiKey = setting.getAiApiKey();
                if (apiKey == null || apiKey.isBlank()) {
                    return Optional.empty();
                }
                
                String baseUrl = setting.getAiBaseUrl();
                String model = setting.getAiModel();
                
                OpenAiApi api;
                if (baseUrl != null && !baseUrl.isBlank()) {
                    api = new OpenAiApi(baseUrl, apiKey);
                } else {
                    api = new OpenAiApi(apiKey);
                }
                
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .withModel(model != null && !model.isBlank() ? model : "gpt-3.5-turbo")
                        .build();
                
                return Optional.of(new OpenAiChatModel(api, options));
            }
            
            return chatModel;
        } catch (Exception e) {
            logger.error("解析ChatModel失败", e);
            return Optional.empty();
        }
    }
}
