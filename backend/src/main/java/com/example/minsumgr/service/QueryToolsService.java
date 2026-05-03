package com.example.minsumgr.service;

import com.example.minsumgr.domain.*;
import com.example.minsumgr.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 查询工具服务 - 提供给AI Function Calling使用的查询功能
 */
@Service
public class QueryToolsService {

    private static final Logger logger = LoggerFactory.getLogger(QueryToolsService.class);
    
    private final BookingRepository bookingRepository;
    private final HomestayRepository homestayRepository;
    private final ActivityRepository activityRepository;
    private final RouteRepository routeRepository;
    private final HomestayTypeRepository homestayTypeRepository;
    private final ObjectMapper objectMapper;

    public QueryToolsService(BookingRepository bookingRepository,
                            HomestayRepository homestayRepository,
                            ActivityRepository activityRepository,
                            RouteRepository routeRepository,
                            HomestayTypeRepository homestayTypeRepository) {
        this.bookingRepository = bookingRepository;
        this.homestayRepository = homestayRepository;
        this.activityRepository = activityRepository;
        this.routeRepository = routeRepository;
        this.homestayTypeRepository = homestayTypeRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 查询用户订单列表
     * @param userId 用户ID
     * @return 订单列表JSON
     */
    public String queryUserBookings(Long userId) {
        logger.info("Function Call: 查询用户订单, userId={}", userId);
        try {
            List<Booking> bookings = bookingRepository.findByUserId(userId);
            
            if (bookings.isEmpty()) {
                return "{\"message\": \"该用户暂无订单记录\", \"bookings\": []}";
            }
            
            List<Map<String, Object>> result = bookings.stream()
                    .map(booking -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("orderId", booking.getId());
                        map.put("homestayId", booking.getHomestayId());
                        
                        Homestay homestay = homestayRepository.findById(booking.getHomestayId()).orElse(null);
                        map.put("homestayName", homestay != null ? homestay.getName() : "未知民宿");
                        
                        map.put("checkInDate", booking.getCheckInDate().toString());
                        map.put("checkOutDate", booking.getCheckOutDate().toString());
                        map.put("status", booking.getStatus());
                        map.put("statusText", getStatusText(booking.getStatus()));
                        map.put("totalPrice", booking.getTotalPrice());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("userId", userId);
            response.put("totalOrders", result.size());
            response.put("bookings", result);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("查询用户订单失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询所有民宿列表
     * @return 民宿列表JSON
     */
    public String queryAllHomestays() {
        logger.info("Function Call: 查询所有民宿");
        try {
            List<Homestay> homestays = homestayRepository.findAll();
            
            if (homestays.isEmpty()) {
                return "{\"message\": \"暂无民宿信息\", \"homestays\": []}";
            }
            
            List<Map<String, Object>> result = homestays.stream()
                    .map(homestay -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", homestay.getId());
                        map.put("name", homestay.getName());
                        map.put("location", homestay.getLocation());
                        map.put("description", homestay.getDescription());
                        map.put("pricePerNight", homestay.getPricePerNight());
                        map.put("availableRooms", homestay.getAvailableRooms());
                        
                        if (homestay.getTypeId() != null) {
                            HomestayType type = homestayTypeRepository.findById(homestay.getTypeId()).orElse(null);
                            map.put("typeName", type != null ? type.getName() : null);
                        }
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("total", result.size());
            response.put("homestays", result);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("查询民宿列表失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询指定民宿详情
     * @param homestayId 民宿ID
     * @return 民宿详情JSON
     */
    public String queryHomestayDetail(Long homestayId) {
        logger.info("Function Call: 查询民宿详情, homestayId={}", homestayId);
        try {
            Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
            
            if (homestay == null) {
                return "{\"error\": \"民宿不存在\", \"homestayId\": " + homestayId + "}";
            }
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", homestay.getId());
            result.put("name", homestay.getName());
            result.put("location", homestay.getLocation());
            result.put("description", homestay.getDescription());
            result.put("pricePerNight", homestay.getPricePerNight());
            result.put("availableRooms", homestay.getAvailableRooms());
            result.put("roomNotes", homestay.getRoomNotes());
            
            if (homestay.getTypeId() != null) {
                HomestayType type = homestayTypeRepository.findById(homestay.getTypeId()).orElse(null);
                result.put("typeName", type != null ? type.getName() : null);
            }
            
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询民宿详情失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询民宿在指定日期范围内的可用情况
     * @param homestayId 民宿ID
     * @param startDate 开始日期 (yyyy-MM-dd)
     * @param endDate 结束日期 (yyyy-MM-dd)
     * @return 可用情况JSON
     */
    public String queryHomestayAvailability(Long homestayId, String startDate, String endDate) {
        logger.info("Function Call: 查询民宿可用情况, homestayId={}, {} - {}", homestayId, startDate, endDate);
        try {
            Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
            
            if (homestay == null) {
                return "{\"error\": \"民宿不存在\"}";
            }
            
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            
            // 查询该时间段内的已确认预订
            List<Booking> bookings = bookingRepository.findByHomestayIdAndStatus(homestayId, "CONFIRMED");
            
            // 过滤出与指定日期范围有重叠的预订
            List<Booking> conflictBookings = bookings.stream()
                    .filter(b -> !b.getCheckOutDate().isBefore(start) && !b.getCheckInDate().isAfter(end))
                    .collect(Collectors.toList());
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("homestayId", homestayId);
            result.put("homestayName", homestay.getName());
            result.put("dateRange", startDate + " 至 " + endDate);
            result.put("totalRooms", homestay.getAvailableRooms());
            result.put("conflictBookings", conflictBookings.size());
            result.put("isAvailable", conflictBookings.isEmpty() || 
                       (homestay.getAvailableRooms() != null && homestay.getAvailableRooms() > conflictBookings.size()));
            result.put("pricePerNight", homestay.getPricePerNight());
            
            if (!conflictBookings.isEmpty()) {
                result.put("existingBookings", conflictBookings.stream()
                        .map(b -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("checkIn", b.getCheckInDate().toString());
                            m.put("checkOut", b.getCheckOutDate().toString());
                            return m;
                        })
                        .collect(Collectors.toList()));
            }
            
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询民宿可用情况失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询所有游玩活动
     * @return 活动列表JSON
     */
    public String queryAllActivities() {
        logger.info("Function Call: 查询所有游玩活动");
        try {
            List<Activity> activities = activityRepository.findAll();
            
            if (activities.isEmpty()) {
                return "{\"message\": \"暂无游玩活动\", \"activities\": []}";
            }
            
            List<Map<String, Object>> result = activities.stream()
                    .map(activity -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", activity.getId());
                        map.put("name", activity.getName());
                        map.put("description", activity.getDescription());
                        map.put("location", activity.getLocation());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("total", result.size());
            response.put("activities", result);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("查询游玩活动失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询所有旅游路线
     * @return 路线列表JSON
     */
    public String queryAllRoutes() {
        logger.info("Function Call: 查询所有旅游路线");
        try {
            List<Route> routes = routeRepository.findAll();
            
            if (routes.isEmpty()) {
                return "{\"message\": \"暂无旅游路线\", \"routes\": []}";
            }
            
            List<Map<String, Object>> result = routes.stream()
                    .map(route -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", route.getId());
                        map.put("name", route.getName());
                        map.put("description", route.getDescription());
                        map.put("tags", route.getTags());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("total", result.size());
            response.put("routes", result);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("查询旅游路线失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询订单详情
     * @param bookingId 订单ID
     * @return 订单详情JSON
     */
    public String queryBookingDetail(Long bookingId) {
        logger.info("Function Call: 查询订单详情, bookingId={}", bookingId);
        try {
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            
            if (booking == null) {
                return "{\"error\": \"订单不存在\", \"bookingId\": " + bookingId + "}";
            }
            
            Homestay homestay = homestayRepository.findById(booking.getHomestayId()).orElse(null);
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderId", booking.getId());
            result.put("userId", booking.getUserId());
            result.put("homestayId", booking.getHomestayId());
            result.put("homestayName", homestay != null ? homestay.getName() : "未知民宿");
            result.put("checkInDate", booking.getCheckInDate().toString());
            result.put("checkOutDate", booking.getCheckOutDate().toString());
            result.put("status", booking.getStatus());
            result.put("statusText", getStatusText(booking.getStatus()));
            result.put("totalPrice", booking.getTotalPrice());
            result.put("guestCount", booking.getGuestCount());
            result.put("specialRequests", booking.getSpecialRequests());
            result.put("createdAt", booking.getCreatedAt() != null ? booking.getCreatedAt().toString() : null);
            
            if (homestay != null) {
                result.put("homestayLocation", homestay.getLocation());
                result.put("roomNotes", homestay.getRoomNotes());
            }
            
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("查询订单详情失败", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 按关键词搜索民宿
     * @param keyword 搜索关键词
     * @return 匹配的民宿列表JSON
     */
    public String searchHomestays(String keyword) {
        logger.info("Function Call: 搜索民宿, keyword={}", keyword);
        try {
            List<Homestay> allHomestays = homestayRepository.findAll();
            String lowerKeyword = keyword.toLowerCase();
            
            List<Map<String, Object>> result = allHomestays.stream()
                    .filter(h -> (h.getName() != null && h.getName().toLowerCase().contains(lowerKeyword)) ||
                                 (h.getLocation() != null && h.getLocation().toLowerCase().contains(lowerKeyword)) ||
                                 (h.getDescription() != null && h.getDescription().toLowerCase().contains(lowerKeyword)))
                    .map(homestay -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", homestay.getId());
                        map.put("name", homestay.getName());
                        map.put("location", homestay.getLocation());
                        map.put("description", homestay.getDescription());
                        map.put("pricePerNight", homestay.getPricePerNight());
                        map.put("availableRooms", homestay.getAvailableRooms());
                        return map;
                    })
                    .collect(Collectors.toList());
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("keyword", keyword);
            response.put("matchCount", result.size());
            response.put("homestays", result);
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("搜索民宿失败", e);
            return "{\"error\": \"搜索失败: " + e.getMessage() + "\"}";
        }
    }

    private String getStatusText(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "PENDING" -> "待确认";
            case "CONFIRMED" -> "已确认";
            case "CANCELLED" -> "已取消";
            case "COMPLETED" -> "已完成";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    /**
     * 创建民宿订单草稿（AI Function Call 使用）
     * @param userId 用户ID
     * @param homestayId 民宿ID
     * @param checkInDate 入住日期 (yyyy-MM-dd)
     * @param checkOutDate 退房日期 (yyyy-MM-dd)
     * @param guestCount 入住人数
     * @param recommendReason AI推荐理由
     * @return 订单草稿信息JSON
     */
    public String createBookingDraft(Long userId, Long homestayId, String checkInDate, 
                                     String checkOutDate, Integer guestCount, String recommendReason) {
        logger.info("Function Call: 创建订单草稿, userId={}, homestayId={}, {} - {}", 
                   userId, homestayId, checkInDate, checkOutDate);
        try {
            Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
            if (homestay == null) {
                return "{\"success\": false, \"error\": \"民宿不存在\", \"homestayId\": " + homestayId + "}";
            }
            
            LocalDate inDate = LocalDate.parse(checkInDate);
            LocalDate outDate = LocalDate.parse(checkOutDate);
            
            // 检查日期有效性
            if (!outDate.isAfter(inDate)) {
                return "{\"success\": false, \"error\": \"退房日期必须晚于入住日期\"}";
            }
            
            // 计算天数和总价
            long nights = java.time.temporal.ChronoUnit.DAYS.between(inDate, outDate);
            java.math.BigDecimal totalPrice = homestay.getPricePerNight()
                    .multiply(java.math.BigDecimal.valueOf(nights));
            
            // 检查房间可用性
            List<Booking> conflictingBookings = bookingRepository.findByHomestayIdAndStatus(homestayId, "CONFIRMED").stream()
                    .filter(b -> !(b.getCheckOutDate().isBefore(inDate) || b.getCheckInDate().isAfter(outDate.minusDays(1))))
                    .collect(Collectors.toList());
            
            int availableRooms = homestay.getAvailableRooms() != null ? homestay.getAvailableRooms() : 0;
            boolean isAvailable = conflictingBookings.size() < availableRooms;
            
            logger.info("===== 房间可用性检查 =====");
            logger.info("民宿: {} (ID={})", homestay.getName(), homestayId);
            logger.info("日期范围: {} 到 {}", checkInDate, checkOutDate);
            logger.info("总可用房间数: {}", availableRooms);
            logger.info("冲突预订数: {}", conflictingBookings.size());
            logger.info("是否可用: {}", isAvailable);
            logger.info("=========================");
            
            // 生成唯一的选项ID
            String optionId = "draft_" + homestayId + "_" + System.currentTimeMillis();
            
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("optionId", optionId);
            result.put("homestayId", homestayId);
            result.put("homestayName", homestay.getName());
            result.put("homestayLocation", homestay.getLocation());
            result.put("homestayImageUrl", homestay.getImageUrl());
            result.put("checkInDate", checkInDate);
            result.put("checkOutDate", checkOutDate);
            result.put("nights", nights);
            result.put("pricePerNight", homestay.getPricePerNight());
            result.put("totalPrice", totalPrice);
            result.put("guestCount", guestCount != null ? guestCount : 1);
            result.put("availableRooms", availableRooms);
            result.put("currentlyBooked", conflictingBookings.size());
            result.put("isAvailable", isAvailable);
            result.put("roomNotes", homestay.getRoomNotes());
            result.put("recommendReason", recommendReason);
            result.put("userId", userId);
            
            if (!isAvailable) {
                result.put("warning", "该时间段房间已满，建议选择其他日期或民宿");
            }
            
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            logger.error("创建订单草稿失败", e);
            return "{\"success\": false, \"error\": \"创建失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 批量创建民宿订单草稿（多个推荐方案）
     * @param userId 用户ID
     * @param draftsJson 订单草稿列表的JSON数组
     * @return 多个订单草稿信息JSON
     */
    public String createMultipleBookingDrafts(Long userId, String draftsJson) {
        logger.info("Function Call: 批量创建订单草稿, userId={}", userId);
        try {
            // 解析传入的草稿列表
            List<Map<String, Object>> drafts = objectMapper.readValue(draftsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            
            List<Map<String, Object>> results = new ArrayList<>();
            
            for (Map<String, Object> draft : drafts) {
                Long homestayId = Long.valueOf(draft.get("homestayId").toString());
                String checkInDate = draft.get("checkInDate").toString();
                String checkOutDate = draft.get("checkOutDate").toString();
                Integer guestCount = draft.get("guestCount") != null ? 
                        Integer.valueOf(draft.get("guestCount").toString()) : 1;
                String recommendReason = draft.get("recommendReason") != null ? 
                        draft.get("recommendReason").toString() : "";
                
                String draftResult = createBookingDraft(userId, homestayId, checkInDate, 
                        checkOutDate, guestCount, recommendReason);
                Map<String, Object> parsed = objectMapper.readValue(draftResult, Map.class);
                results.add(parsed);
            }
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("userId", userId);
            response.put("totalOptions", results.size());
            response.put("options", results);
            response.put("message", results.size() == 1 ? 
                    "已为您生成1个预订方案，请确认订单信息" : 
                    "已为您生成" + results.size() + "个预订方案，请选择您心仪的民宿并确认订单");
            
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("批量创建订单草稿失败", e);
            return "{\"success\": false, \"error\": \"批量创建失败: " + e.getMessage() + "\"}";
        }
    }
}
