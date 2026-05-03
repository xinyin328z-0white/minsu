package com.example.minsumgr.repository;

import com.example.minsumgr.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    List<Booking> findByHomestayIdAndStatus(Long homestayId, String status);
    List<Booking> findByUserIdAndStatus(Long userId, String status);
}
