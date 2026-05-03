package com.example.minsumgr.config;

import com.example.minsumgr.service.QueryToolsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * AI Function Calling 配置
 * 定义可供AI调用的查询工具函数
 */
@Configuration
public class AiFunctionConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiFunctionConfig.class);

    private final QueryToolsService queryToolsService;

    public AiFunctionConfig(QueryToolsService queryToolsService) {
        this.queryToolsService = queryToolsService;
    }

    // ========== Function Input Records ==========

    public record UserIdRequest(Long userId) {}
    public record HomestayIdRequest(Long homestayId) {}
    public record BookingIdRequest(Long bookingId) {}
    public record KeywordRequest(String keyword) {}
    public record AvailabilityRequest(Long homestayId, String startDate, String endDate) {}
    public record EmptyRequest() {}
    public record BookingDraftRequest(Long userId, Long homestayId, String checkInDate, 
                                       String checkOutDate, Integer guestCount, String recommendReason) {}
    public record MultipleBookingDraftsRequest(Long userId, String draftsJson) {}

    // ========== Function Beans ==========

    @Bean
    @Description("查询指定用户的所有订单记录，包含订单ID、民宿名称、入住日期、退房日期、状态、价格等信息")
    public Function<UserIdRequest, String> queryUserBookings() {
        return request -> {
            logger.info("AI调用queryUserBookings: userId={}", request.userId());
            return queryToolsService.queryUserBookings(request.userId());
        };
    }

    @Bean
    @Description("查询所有民宿列表，返回民宿ID、名称、位置、描述、每晚价格、可用房间数等信息")
    public Function<EmptyRequest, String> queryAllHomestays() {
        return request -> {
            logger.info("AI调用queryAllHomestays");
            return queryToolsService.queryAllHomestays();
        };
    }

    @Bean
    @Description("查询指定民宿的详细信息，包含名称、位置、描述、价格、可用房间数、房间注意事项等")
    public Function<HomestayIdRequest, String> queryHomestayDetail() {
        return request -> {
            logger.info("AI调用queryHomestayDetail: homestayId={}", request.homestayId());
            return queryToolsService.queryHomestayDetail(request.homestayId());
        };
    }

    @Bean
    @Description("查询民宿在指定日期范围内的可用情况，判断是否可以预订，以及该时段已有的预订信息")
    public Function<AvailabilityRequest, String> queryHomestayAvailability() {
        return request -> {
            logger.info("AI调用queryHomestayAvailability: homestayId={}, {} - {}", 
                       request.homestayId(), request.startDate(), request.endDate());
            return queryToolsService.queryHomestayAvailability(
                    request.homestayId(), request.startDate(), request.endDate());
        };
    }

    @Bean
    @Description("查询所有游玩活动项目列表，返回活动ID、名称、描述、地点等信息")
    public Function<EmptyRequest, String> queryAllActivities() {
        return request -> {
            logger.info("AI调用queryAllActivities");
            return queryToolsService.queryAllActivities();
        };
    }

    @Bean
    @Description("查询所有旅游路线列表，返回路线ID、名称、描述、标签等信息")
    public Function<EmptyRequest, String> queryAllRoutes() {
        return request -> {
            logger.info("AI调用queryAllRoutes");
            return queryToolsService.queryAllRoutes();
        };
    }

    @Bean
    @Description("查询指定订单的详细信息，包含订单ID、用户ID、民宿信息、入住退房日期、状态、价格、客人数量、特殊要求等")
    public Function<BookingIdRequest, String> queryBookingDetail() {
        return request -> {
            logger.info("AI调用queryBookingDetail: bookingId={}", request.bookingId());
            return queryToolsService.queryBookingDetail(request.bookingId());
        };
    }

    @Bean
    @Description("按关键词搜索民宿，在民宿名称、位置、描述中进行模糊匹配")
    public Function<KeywordRequest, String> searchHomestays() {
        return request -> {
            logger.info("AI调用searchHomestays: keyword={}", request.keyword());
            return queryToolsService.searchHomestays(request.keyword());
        };
    }

    @Bean
    @Description("为用户创建民宿预订草稿，生成待确认的订单方案。参数：userId（用户ID）、homestayId（民宿ID）、checkInDate（入住日期，格式yyyy-MM-dd）、checkOutDate（退房日期，格式yyyy-MM-dd）、guestCount（入住人数）、recommendReason（推荐理由）")
    public Function<BookingDraftRequest, String> createBookingDraft() {
        return request -> {
            logger.info("AI调用createBookingDraft: userId={}, homestayId={}, {} - {}", 
                       request.userId(), request.homestayId(), request.checkInDate(), request.checkOutDate());
            return queryToolsService.createBookingDraft(
                    request.userId(), request.homestayId(), request.checkInDate(),
                    request.checkOutDate(), request.guestCount(), request.recommendReason());
        };
    }

    @Bean
    @Description("批量创建多个民宿预订草稿，为用户提供多个可选方案。当需要推荐多个民宿供用户选择时使用。参数：userId（用户ID）、draftsJson（订单草稿数组的JSON字符串，每个元素包含homestayId、checkInDate、checkOutDate、guestCount、recommendReason）")
    public Function<MultipleBookingDraftsRequest, String> createMultipleBookingDrafts() {
        return request -> {
            logger.info("AI调用createMultipleBookingDrafts: userId={}", request.userId());
            return queryToolsService.createMultipleBookingDrafts(request.userId(), request.draftsJson());
        };
    }
}
