package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.domain.Message;
import com.example.minsumgr.domain.User;
import com.example.minsumgr.repository.MessageRepository;
import com.example.minsumgr.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "http://localhost:5173")
public class MessageController {

    private final MessageRepository repository;
    private final UserRepository userRepository;

    public MessageController(MessageRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private void fillCustomerName(Message message) {
        if (message.getCustomerName() == null || message.getCustomerName().trim().isEmpty()) {
            if (message.getUserId() != null) {
                Optional<User> user = userRepository.findById(message.getUserId());
                if (user.isPresent()) {
                    // 优先使用真实姓名，其次是用户名
                    String customerName = user.get().getRealName() != null ? user.get().getRealName() : user.get().getUsername();
                    message.setCustomerName(customerName);
                }
            }
        }
    }

    @GetMapping
    @RequireRole(roles = "ADMIN")
    public List<Message> list() {
        List<Message> messages = repository.findAll();
        messages.forEach(this::fillCustomerName);
        return messages;
    }

    @GetMapping("/user/{userId}")
    @RequireRole
    public List<Message> listByUser(@PathVariable Long userId) {
        List<Message> messages = repository.findByUserId(userId);
        messages.forEach(this::fillCustomerName);
        return messages;
    }

    @PostMapping
    @RequireRole
    public Message create(@RequestBody Message message) {
        fillCustomerName(message);
        return repository.save(message);
    }

    @PutMapping("/{id}")
    public Message update(@PathVariable Long id, @RequestBody Message message) {
        message.setId(id);
        fillCustomerName(message);
        return repository.save(message);
    }

    @PutMapping("/user/{userId}/mark-read")
    @RequireRole
    public void markUserMessagesAsRead(@PathVariable Long userId) {
        List<Message> unreadMessages = repository.findByUserIdAndTypeAndIsReadFalse(userId, "CUSTOMER_INQUIRY");
        unreadMessages.forEach(msg -> msg.setIsRead(true));
        repository.saveAll(unreadMessages);
    }

    @PutMapping("/user/{userId}/mark-admin-read")
    public void markUserAdminMessagesAsRead(@PathVariable Long userId) {
        List<Message> unreadMessages = repository.findByUserIdAndTypeAndIsReadFalse(userId, "ADMIN_REPLY");
        unreadMessages.forEach(msg -> msg.setIsRead(true));
        repository.saveAll(unreadMessages);
    }

    @PostMapping("/fill-customer-names")
    public void fillAllCustomerNames() {
        List<Message> allMessages = repository.findAll();
        allMessages.forEach(this::fillCustomerName);
        repository.saveAll(allMessages);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
