package com.neovarsity.toursattractions.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovarsity.toursattractions.exception.ErrorResponse;
import com.neovarsity.toursattractions.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String method = request.getMethod();
        String uri = request.getRequestURI();

        boolean needsAuth = ("POST".equals(method) && uri.equals("/api/v1/bookings"))
                || ("POST".equals(method) && uri.equals("/api/v1/reviews"))
                || (uri.startsWith("/api/v1/bookings/customer/"));

        if (!needsAuth) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (authService.getSessionUserId(session) != null) {
            return true;
        }

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(401)
                .error("Unauthorized")
                .message("Please log in to continue")
                .path(uri)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
