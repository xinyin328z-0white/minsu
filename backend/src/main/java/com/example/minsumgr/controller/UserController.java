package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.domain.User;
import com.example.minsumgr.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @RequireRole(roles = "ADMIN")
    public List<User> list() {
        return userRepository.findAll();
    }

    @GetMapping("/{id}")
    @RequireRole(roles = {"ADMIN", "CUSTOMER"})
    public User get(@PathVariable Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @PostMapping
    @RequireRole(roles = "ADMIN")
    public User create(@RequestBody User user) {
        return userRepository.save(user);
    }

    @PutMapping("/{id}")
    @RequireRole(roles = "ADMIN")
    public User update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        return userRepository.save(user);
    }
    
    @PutMapping("/{id}/profile")
    @RequireRole(roles = {"ADMIN", "CUSTOMER"})
    public User updateProfile(@PathVariable Long id, @RequestBody User profileUpdate) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        
        // 仅更新个人资料字段，不更新密码或角色
        if (profileUpdate.getRealName() != null) user.setRealName(profileUpdate.getRealName());
        if (profileUpdate.getPhone() != null) user.setPhone(profileUpdate.getPhone());
        if (profileUpdate.getEmail() != null) user.setEmail(profileUpdate.getEmail());
        if (profileUpdate.getIdCard() != null) user.setIdCard(profileUpdate.getIdCard());
        if (profileUpdate.getAddress() != null) user.setAddress(profileUpdate.getAddress());
        if (profileUpdate.getAvatar() != null) user.setAvatar(profileUpdate.getAvatar());
        
        return userRepository.save(user);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userRepository.deleteById(id);
    }
}
