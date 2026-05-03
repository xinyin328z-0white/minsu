package com.example.minsumgr.service;

import com.example.minsumgr.domain.Booking;
import com.example.minsumgr.domain.Homestay;
import com.example.minsumgr.repository.BookingRepository;
import com.example.minsumgr.repository.HomestayRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final HomestayRepository homestayRepository;

    public BookingService(BookingRepository bookingRepository, HomestayRepository homestayRepository) {
        this.bookingRepository = bookingRepository;
        this.homestayRepository = homestayRepository;
    }

    /**
     * 检查是否存在房间冲突
     * 如果在给定时间段内，已确认的订单数量达到房间总数，则存在冲突
     */
    public boolean hasRoomConflict(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
        if (homestay == null || homestay.getAvailableRooms() == null) {
            return false;
        }

        // 查询在指定时间段内的已确认订单
        List<Booking> confirmedBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getHomestayId().equals(homestayId))
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .filter(b -> {
                    // 检查时间是否重叠
                    return !(b.getCheckOutDate().isBefore(checkInDate) || 
                             b.getCheckInDate().isAfter(checkOutDate));
                })
                .toList();

        // 如果已确认的订单数等于房间总数，则没有可用房间
        return confirmedBookings.size() >= homestay.getAvailableRooms();
    }

    /**
     * 获取某个民宿在指定时间段内的已预订房间数
     */
    public Integer getBookedRoomsInPeriod(Long homestayId, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Booking> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getHomestayId().equals(homestayId))
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .filter(b -> {
                    // 检查时间是否重叠
                    return !(b.getCheckOutDate().isBefore(checkInDate) || 
                             b.getCheckInDate().isAfter(checkOutDate));
                })
                .toList();
        
        return bookings.size();
    }

    /**
     * 获取指定日期的房间可用数量
     * @param homestayId 民宿ID
     * @param date 查询日期
     * @return 该日期的可用房间数
     */
    public Integer getAvailableRoomsForDate(Long homestayId, LocalDate date) {
        Homestay homestay = homestayRepository.findById(homestayId).orElse(null);
        if (homestay == null || homestay.getAvailableRooms() == null) {
            return 0;
        }

        // 查询在该日期有订单的已确认预订
        List<Booking> bookingsOnDate = bookingRepository.findAll().stream()
                .filter(b -> b.getHomestayId().equals(homestayId))
                .filter(b -> "CONFIRMED".equals(b.getStatus()))
                .filter(b -> {
                    // 检查日期是否在预订范围内（包含入住日期，不包含退房日期）
                    return !date.isBefore(b.getCheckInDate()) && date.isBefore(b.getCheckOutDate());
                })
                .toList();

        return Math.max(0, homestay.getAvailableRooms() - bookingsOnDate.size());
    }
}
