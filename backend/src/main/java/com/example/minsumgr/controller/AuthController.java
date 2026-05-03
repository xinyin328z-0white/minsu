package com.example.minsumgr.controller;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.domain.User;
import com.example.minsumgr.dto.LoginRequest;
import com.example.minsumgr.dto.LoginResponse;
import com.example.minsumgr.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 处理登录、注册、Token刷新等
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public LoginResponse register(@RequestBody User user) {
        return authService.register(user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * 验证Token (无需权限)
     */
    @GetMapping("/verify")
    public User verify(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return authService.validateToken(token);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh-token")
    public LoginResponse refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalArgumentException("刷新Token不能为空");
        }
        return authService.refreshToken(refreshToken);
    }

    /**
     * 登出 - 仅用于前端清除本地Token
     */
    @PostMapping("/logout")
    @RequireRole(roles = {"ADMIN", "CUSTOMER"})
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "已成功登出");
        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户信息 - 需要认证
     */
    @GetMapping("/me")
    @RequireRole(roles = {"ADMIN", "CUSTOMER"})
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        // 这里实际的用户信息应该从request attributes中获取
        // 由JwtInterceptor注入
        Map<String, Object> response = new HashMap<>();
        response.put("message", "已认证");
        return ResponseEntity.ok(response);
    }
}
