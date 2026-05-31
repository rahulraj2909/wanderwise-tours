package com.neovarsity.toursattractions.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovarsity.toursattractions.client.BookingSessionClient;
import com.neovarsity.toursattractions.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ReviewAuthInterceptor implements HandlerInterceptor {

    private final BookingSessionClient bookingSessionClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/v1/reviews".equals(request.getRequestURI())) {
            return true;
        }
        if (bookingSessionClient.resolveUser(request).isPresent()) {
            return true;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(401)
                .error("Unauthorized")
                .message("Please log in to post a review")
                .path(request.getRequestURI())
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
