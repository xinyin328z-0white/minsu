package com.example.minsumgr.service;

import com.example.minsumgr.domain.SystemSetting;
import com.example.minsumgr.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemSettingService {

    private static final int DEFAULT_BOOKING_WINDOW_DAYS = 60;

    private final SystemSettingRepository repository;

    public SystemSettingService(SystemSettingRepository repository) {
        this.repository = repository;
    }

    public SystemSetting getOrCreate() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            SystemSetting setting = new SystemSetting();
            setting.setBookingWindowDays(DEFAULT_BOOKING_WINDOW_DAYS);
            return repository.save(setting);
        });
    }

    public int getBookingWindowDays() {
        SystemSetting setting = getOrCreate();
        Integer days = setting.getBookingWindowDays();
        return days == null ? DEFAULT_BOOKING_WINDOW_DAYS : days;
    }

    public SystemSetting updateBookingWindowDays(Integer days) {
        SystemSetting setting = getOrCreate();
        setting.setBookingWindowDays(days);
        return repository.save(setting);
    }

    public SystemSetting getSystemSetting() {
        return getOrCreate();
    }

    public SystemSetting updateSystemSetting(SystemSetting setting) {
        return repository.save(setting);
    }
}
