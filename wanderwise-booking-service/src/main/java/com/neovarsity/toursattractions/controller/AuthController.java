package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.request.LoginRequest;
import com.neovarsity.toursattractions.dto.request.RegisterRequest;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.dto.response.UserResponse;
import com.neovarsity.toursattractions.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(@Valid @RequestBody RegisterRequest request, HttpSession session) {
        UserResponse user = authService.register(request);
        session.setAttribute(AuthService.SESSION_USER_ID, user.getId());
        return ApiResponse.ok("Account created", user);
    }

    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return ApiResponse.ok("Logged in", authService.login(request, session));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        authService.logout(session);
        return ApiResponse.ok("Logged out", null);
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(HttpSession session) {
        UserResponse user = authService.currentUser(session);
        if (user == null) {
            return ApiResponse.<UserResponse>builder().success(true).data(null).build();
        }
        return ApiResponse.ok(user);
    }
}
