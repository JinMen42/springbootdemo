package com.example.springbootdemo.interceptor;

import com.example.springbootdemo.common.Result;
import com.example.springbootdemo.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, @Nullable Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            writeError(response, "未登录，请先携带 token");
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!JwtUtils.isTokenValid(token)) {
            writeError(response, "token 无效或已过期");
            return false;
        }

        request.setAttribute("currentUsername", JwtUtils.getUsername(token));
        request.setAttribute("currentUserId", JwtUtils.getUserId(token));

        return true;
    }

    private void writeError(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Result<String> result = Result.fail(401, msg);
        String json = new ObjectMapper().writeValueAsString(result);
        response.getWriter().write(json);
    }
}