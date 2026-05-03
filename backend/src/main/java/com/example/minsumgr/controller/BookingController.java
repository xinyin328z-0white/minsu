package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.domain.Booking;
import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.domain.Message;
import com.example.minsumgr.domain.User;
import com.example.minsumgr.dto.BookingConfirmRequest;
import com.example.minsumgr.repository.BookingRepository;
import com.example.minsumgr.repository.MessageRepository;
import com.example.minsumgr.repository.UserRepository;
import com.example.minsumgr.repository.HomestayRepository;
import com.example.minsumgr.service.BookingService;
import com.example.minsumgr.service.SystemSettingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "http://localhost:5173")
public class BookingController {

    private final BookingRepository repository;
    private final BookingService bookingService;
    private final SystemSettingService systemSettingService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final HomestayRepository homestayRepository;

    public BookingController(BookingRepository repository, BookingService bookingService, 
                           SystemSettingService systemSettingService, MessageRepository messageRepository,
                           UserRepository userRepository, HomestayRepository homestayRepository) {
        this.repository = repository;
        this.bookingService = bookingService;
        this.systemSettingService = systemSettingService;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.homestayRepository = homestayRepository;
    }

    @GetMapping
    public List<Booking> list() {
        return repository.findAll();
    }

    @GetMapping("/user/{userId}")
    @RequireRole
    public List<Booking> listByUser(@PathVariable Long userId) {
        return repository.findByUserId(userId);
    }

    @GetMapping("/homestay/{homestayId}/confirmed")
    public List<Booking> listConfirmedByHomestay(@PathVariable Long homestayId) {
        return repository.findByHomestayIdAndStatus(homestayId, "CONFIRMED");
    }

    @PostMapping("/check-conflict")
    @RequireRole
    public Map<String, Object> checkRoomConflict(
            @RequestParam Long homestayId,
            @RequestParam LocalDate checkInDate,
            @RequestParam LocalDate checkOutDate) {
        Map<String, Object> result = new HashMap<>();
        boolean hasConflict = bookingService.hasRoomConflict(homestayId, checkInDate, checkOutDate);
        Integer bookedRooms = bookingService.getBookedRoomsInPeriod(homestayId, checkInDate, checkOutDate);
        
        result.put("hasConflict", hasConflict);
        result.put("bookedRooms", bookedRooms);
        result.put("message", hasConflict ? "该时间段房间已满，请选择其他日期" : "房间可用");
        
        return result;
    }

    @GetMapping("/room-calendar/{homestayId}")
    public Map<String, Object> getRoomCalendar(
            @PathVariable Long homestayId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Integer> dailyAvailability = new HashMap<>();
        
        // 遇每一天，计算余房
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            int available = bookingService.getAvailableRoomsForDate(homestayId, date);
            dailyAvailability.put(date.toString(), available);
        }
        
        result.put("homestayId", homestayId);
        result.put("dailyAvailability", dailyAvailability);
        return result;
    }

    @PostMapping
    @RequireRole
    public Booking create(@RequestBody Booking booking) {
        int windowDays = systemSettingService.getBookingWindowDays();
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(windowDays);
        if (booking.getCheckInDate() != null && booking.getCheckInDate().isAfter(maxDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可预订" + windowDays + "天内的日期");
        }
        if (booking.getCheckOutDate() != null && booking.getCheckOutDate().isAfter(maxDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可预订" + windowDays + "天内的日期");
        }
        
        // 如果没有设置status，默认为PENDING
        if (booking.getStatus() == null || booking.getStatus().isEmpty()) {
            booking.setStatus("PENDING");
        }
        
        // 如果没有设置totalPrice，则基于民宿价格和入住天数计算
        if (booking.getTotalPrice() == null || booking.getTotalPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            if (booking.getHomestayId() != null && booking.getCheckInDate() != null && booking.getCheckOutDate() != null) {
                Homestay homestay = homestayRepository.findById(booking.getHomestayId()).orElse(null);
                if (homestay != null && homestay.getPricePerNight() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
                    java.math.BigDecimal totalPrice = homestay.getPricePerNight().multiply(java.math.BigDecimal.valueOf(Math.max(days, 1)));
                    booking.setTotalPrice(totalPrice);
                }
            }
        }
        
        // 保存预定
        Booking savedBooking = repository.save(booking);
        
        // 自动发送预定通知消息
        try {
            User user = userRepository.findById(booking.getUserId()).orElse(null);
            if (user != null) {
                String homestayName = "未知民宿";
                if (booking.getHomestayId() != null) {
                    homestayName = homestayRepository.findById(booking.getHomestayId())
                            .map(h -> h.getName())
                            .orElse("未知民宿");
                }
                
                String messageContent = String.format(
                    "📅 新建预定通知\n" +
                    "用户: %s\n" +
                    "民宿: %s\n" +
                    "入住: %s\n" +
                    "退房: %s\n" +
                    "人数: %d\n" +
                    "总价: ¥%s\n" +
                    "状态: %s",
                    user.getRealName() != null ? user.getRealName() : user.getUsername(),
                    homestayName,
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getGuestCount() != null ? booking.getGuestCount() : 1,
                    savedBooking.getTotalPrice() != null ? savedBooking.getTotalPrice() : "未设置",
                    booking.getStatus()
                );
                
                Message message = new Message();
                message.setUserId(booking.getUserId());
                message.setCustomerName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                message.setContent(messageContent);
                message.setType("CUSTOMER_INQUIRY");
                message.setIsRead(false);
                
                messageRepository.save(message);
            }
        } catch (Exception e) {
            // 消息发送失败不影响预定创建
            System.err.println("发送预定消息失败: " + e.getMessage());
        }
        
        return savedBooking;
    }

    @GetMapping("/user/{userId}/for-review")
    @RequireRole
    public List<Map<String, Object>> getUserBookingsForReview(@PathVariable Long userId) {
        // 返回用户的所有待审核（PENDING状态）的订单，包含民宿名称
        List<Booking> bookings = repository.findByUserIdAndStatus(userId, "PENDING");
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (Booking booking : bookings) {
            Map<String, Object> bookingMap = new java.util.LinkedHashMap<>();
            bookingMap.put("id", booking.getId());
            bookingMap.put("userId", booking.getUserId());
            bookingMap.put("homestayId", booking.getHomestayId());
            // 将 LocalDate 转换为字符串以避免 Jackson 序列化问题
            bookingMap.put("checkInDate", booking.getCheckInDate() != null ? booking.getCheckInDate().toString() : null);
            bookingMap.put("checkOutDate", booking.getCheckOutDate() != null ? booking.getCheckOutDate().toString() : null);
            bookingMap.put("status", booking.getStatus());
            bookingMap.put("totalPrice", booking.getTotalPrice());
            bookingMap.put("guestCount", booking.getGuestCount());
            bookingMap.put("contactPhone", booking.getContactPhone());
            bookingMap.put("specialRequests", booking.getSpecialRequests());
            
            // 添加民宿名称
            if (booking.getHomestayId() != null) {
                String homestayName = homestayRepository.findById(booking.getHomestayId())
                        .map(h -> h.getName())
                        .orElse("未知民宿");
                bookingMap.put("homestayName", homestayName);
            } else {
                bookingMap.put("homestayName", "未知民宿");
            }
            
            result.add(bookingMap);
        }
        
        return result;
    }

    @PostMapping("/{id}/review")
    @RequireRole
    public Map<String, Object> reviewBooking(
            @PathVariable Long id,
            @RequestBody Map<String, String> reviewData) {
        Map<String, Object> result = new HashMap<>();
        
        Booking booking = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
        
        String status = reviewData.get("status");
        String notes = reviewData.get("notes");
        
        if (status == null || (!status.equals("CONFIRMED") && !status.equals("CANCELED"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的审核状态");
        }
        
        // 更新订单状态
        booking.setStatus(status);
        Booking updatedBooking = repository.save(booking);
        
        // 发送审核结果通知消息给用户
        try {
            User user = userRepository.findById(booking.getUserId()).orElse(null);
            if (user != null) {
                String homestayName = "未知民宿";
                if (booking.getHomestayId() != null) {
                    homestayName = homestayRepository.findById(booking.getHomestayId())
                            .map(h -> h.getName())
                            .orElse("未知民宿");
                }
                
                String messageContent;
                if ("CONFIRMED".equals(status)) {
                    messageContent = String.format(
                        "✅ 订单已通过\n" +
                        "民宿: %s\n" +
                        "入住: %s\n" +
                        "退房: %s\n" +
                        "总价: ¥%s\n\n" +
                        "感谢预定，我们期待为您服务！",
                        homestayName,
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        booking.getTotalPrice() != null ? booking.getTotalPrice() : "未设置"
                    );
                } else {
                    messageContent = String.format(
                        "❌ 订单已拒绝\n" +
                        "民宿: %s\n" +
                        "入住: %s\n" +
                        "退房: %s\n\n" +
                        "拒绝原因: %s\n\n" +
                        "如有疑问，请联系我们。",
                        homestayName,
                        booking.getCheckInDate(),
                        booking.getCheckOutDate(),
                        notes != null && !notes.trim().isEmpty() ? notes : "房间已满"
                    );
                }
                
                Message message = new Message();
                message.setUserId(booking.getUserId());
                message.setCustomerName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                message.setContent(messageContent);
                message.setType("ADMIN_REPLY");
                message.setIsRead(false);
                
                messageRepository.save(message);
            }
        } catch (Exception e) {
            System.err.println("发送审核结果消息失败: " + e.getMessage());
        }
        
        // 构建返回的订单数据，包含民宿名称
        Map<String, Object> bookingData = new java.util.LinkedHashMap<>();
        bookingData.put("id", updatedBooking.getId());
        bookingData.put("userId", updatedBooking.getUserId());
        bookingData.put("homestayId", updatedBooking.getHomestayId());
        bookingData.put("checkInDate", updatedBooking.getCheckInDate());
        bookingData.put("checkOutDate", updatedBooking.getCheckOutDate());
        bookingData.put("status", updatedBooking.getStatus());
        bookingData.put("totalPrice", updatedBooking.getTotalPrice());
        bookingData.put("guestCount", updatedBooking.getGuestCount());
        bookingData.put("contactPhone", updatedBooking.getContactPhone());
        bookingData.put("specialRequests", updatedBooking.getSpecialRequests());
        
        // 添加民宿名称
        if (updatedBooking.getHomestayId() != null) {
            String homestayName = homestayRepository.findById(updatedBooking.getHomestayId())
                    .map(h -> h.getName())
                    .orElse("未知民宿");
            bookingData.put("homestayName", homestayName);
        } else {
            bookingData.put("homestayName", "未知民宿");
        }
        
        result.put("success", true);
        result.put("data", bookingData);
        result.put("message", "订单审核已完成");
        
        return result;
    }

    @PutMapping("/{id}")
    public Booking update(@PathVariable Long id, @RequestBody Booking booking) {
        booking.setId(id);
        return repository.save(booking);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }

    /**
     * 确认AI生成的订单草稿，创建正式订单
     * @param request 订单确认请求
     * @return 创建的订单
     */
    @PostMapping("/confirm-ai-draft")
    @RequireRole
    public Map<String, Object> confirmAiBookingDraft(@RequestBody BookingConfirmRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 验证必要字段
            if (request.getUserId() == null || request.getHomestayId() == null || 
                request.getCheckInDate() == null || request.getCheckOutDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少必要的订单信息");
            }
            
            // 检查日期有效性
            if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "退房日期必须晚于入住日期");
            }
            
            // 检查预订窗口期
            int windowDays = systemSettingService.getBookingWindowDays();
            LocalDate today = LocalDate.now();
            LocalDate maxDate = today.plusDays(windowDays);
            if (request.getCheckInDate().isAfter(maxDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅可预订" + windowDays + "天内的日期");
            }
            
            // 检查房间冲突
            boolean hasConflict = bookingService.hasRoomConflict(
                    request.getHomestayId(), request.getCheckInDate(), request.getCheckOutDate());
            if (hasConflict) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该时间段房间已满，请选择其他日期");
            }
            
            // 创建订单
            Booking booking = new Booking();
            booking.setUserId(request.getUserId());
            booking.setHomestayId(request.getHomestayId());
            booking.setCheckInDate(request.getCheckInDate());
            booking.setCheckOutDate(request.getCheckOutDate());
            booking.setGuestCount(request.getGuestCount() != null ? request.getGuestCount() : 1);
            booking.setContactPhone(request.getContactPhone());
            booking.setSpecialRequests(request.getSpecialRequests());
            booking.setTotalPrice(request.getTotalPrice());
            booking.setStatus("PENDING");
            booking.setCreatedAt(LocalDateTime.now());
            booking.setUpdatedAt(LocalDateTime.now());
            
            // 如果没有提供总价，计算价格
            if (booking.getTotalPrice() == null) {
                Homestay homestay = homestayRepository.findById(request.getHomestayId()).orElse(null);
                if (homestay != null && homestay.getPricePerNight() != null) {
                    long days = java.time.temporal.ChronoUnit.DAYS.between(
                            request.getCheckInDate(), request.getCheckOutDate());
                    booking.setTotalPrice(
                            homestay.getPricePerNight().multiply(java.math.BigDecimal.valueOf(days)));
                }
            }
            
            Booking savedBooking = repository.save(booking);
            
            // 发送预订通知消息
            sendBookingNotificationMessage(savedBooking);
            
            result.put("success", true);
            result.put("message", "订单创建成功，等待管理员确认");
            result.put("booking", savedBooking);
            
            return result;
            
        } catch (ResponseStatusException e) {
            throw e; // 重新抛出HTTP异常
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "创建订单失败: " + e.getMessage());
            return result;
        }
    }

    private void sendBookingNotificationMessage(Booking booking) {
        try {
            User user = userRepository.findById(booking.getUserId()).orElse(null);
            if (user != null) {
                String homestayName = "未知民宿";
                if (booking.getHomestayId() != null) {
                    homestayName = homestayRepository.findById(booking.getHomestayId())
                            .map(h -> h.getName())
                            .orElse("未知民宿");
                }
                
                String messageContent = String.format(
                    "🎉 AI智能订单生成成功\n" +
                    "用户: %s\n" +
                    "民宿: %s\n" +
                    "入住: %s\n" +
                    "退房: %s\n" +
                    "人数: %d人\n" +
                    "总价: ¥%s\n" +
                    "状态: 等待确认\n" +
                    "来源: AI智能推荐",
                    user.getRealName() != null ? user.getRealName() : user.getUsername(),
                    homestayName,
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    booking.getGuestCount() != null ? booking.getGuestCount() : 1,
                    booking.getTotalPrice() != null ? booking.getTotalPrice() : "未设置"
                );
                
                Message message = new Message();
                message.setUserId(booking.getUserId());
                message.setCustomerName(user.getRealName() != null ? user.getRealName() : user.getUsername());
                message.setContent(messageContent);
                message.setType("CUSTOMER_INQUIRY");
                message.setIsRead(false);
                
                messageRepository.save(message);
            }
        } catch (Exception e) {
            // 消息发送失败不影响订单创建
            System.err.println("发送AI订单通知失败: " + e.getMessage());
        }
    }
}
