package com.example.minsumgr.service;

import com.example.minsumgr.domain.User;
import com.example.minsumgr.dto.LoginRequest;
import com.example.minsumgr.dto.LoginResponse;
import com.example.minsumgr.repository.UserRepository;
import com.example.minsumgr.util.JwtTokenProvider;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public LoginResponse register(User user) {
        Optional<User> existing = userRepository.findByUsername(user.getUsername());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        // 第一个注册的用户为管理员，之后为客户
        long userCount = userRepository.count();
        user.setRole(userCount == 0 ? "ADMIN" : "CUSTOMER");
        User saved = userRepository.save(user);
        
        String token = jwtTokenProvider.generateToken(saved.getId(), saved.getUsername(), saved.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(saved.getId(), saved.getUsername());
        
        return new LoginResponse(saved.getId(), saved.getUsername(), saved.getRole(), token, refreshToken);
    }

    public LoginResponse login(LoginRequest request) {
        Optional<User> user = userRepository.findByUsername(request.getUsername());
        if (user.isEmpty()) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        User u = user.get();
        if (!u.getPassword().equals(request.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        
        String token = jwtTokenProvider.generateToken(u.getId(), u.getUsername(), u.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(u.getId(), u.getUsername());
        
        return new LoginResponse(u.getId(), u.getUsername(), u.getRole(), token, refreshToken);
    }

    /**
     * 刷新Token
     */
    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("无效的刷新Token");
        }
        
        if (jwtTokenProvider.isTokenExpired(refreshToken)) {
            throw new IllegalArgumentException("刷新Token已过期，请重新登录");
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Optional<User> user = userRepository.findById(userId);
        
        if (user.isEmpty()) {
            throw new IllegalArgumentException("用户不存在");
        }
        
        User u = user.get();
        String newToken = jwtTokenProvider.generateToken(u.getId(), u.getUsername(), u.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(u.getId(), u.getUsername());
        
        return new LoginResponse(u.getId(), u.getUsername(), u.getRole(), newToken, newRefreshToken);
    }

    public User validateToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return null;
        }
        
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        return userRepository.findById(userId).orElse(null);
    }
}
