package com.example.minsumgr.interceptor;

import com.example.minsumgr.annotation.RequireRole;
import com.example.minsumgr.util.JwtTokenProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

/**
 * JWT Token 拦截器
 * 验证请求中的Token有效性和权限
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        
        // 处理OPTIONS请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 检查是否是需要认证的端点
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);

        // 如果没有@RequireRole注解，说明不需要权限检查
        if (requireRole == null) {
            return true;
        }

        // 获取Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorizedResponse(response, "缺少或无效的授权头");
            return false;
        }

        String token = authHeader.substring(7);

        // 验证Token
        if (!jwtTokenProvider.validateToken(token)) {
            sendUnauthorizedResponse(response, "无效的Token");
            return false;
        }

        // 检查Token是否过期
        if (jwtTokenProvider.isTokenExpired(token)) {
            sendUnauthorizedResponse(response, "Token已过期");
            return false;
        }

        // 检查权限
        String userRole = jwtTokenProvider.getRoleFromToken(token);
        if (!Arrays.asList(requireRole.roles()).contains(userRole)) {
            sendForbiddenResponse(response, "权限不足");
            return false;
        }

        // 将用户信息存储在request属性中，便于后续使用
        request.setAttribute("userId", jwtTokenProvider.getUserIdFromToken(token));
        request.setAttribute("username", jwtTokenProvider.getUsernameFromToken(token));
        request.setAttribute("role", userRole);

        return true;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }

    private void sendForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}
