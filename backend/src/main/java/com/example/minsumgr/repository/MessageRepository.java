package com.example.minsumgr.repository;

import com.example.minsumgr.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByUserId(Long userId);
    List<Message> findByType(String type);
    List<Message> findByUserIdAndTypeAndIsReadFalse(Long userId, String type);
}
