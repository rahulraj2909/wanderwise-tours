package com.neovarsity.toursattractions.controller;

import com.neovarsity.toursattractions.dto.request.CreateUserRequest;
import com.neovarsity.toursattractions.dto.response.ApiResponse;
import com.neovarsity.toursattractions.entity.User;
import com.neovarsity.toursattractions.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<User> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok("User created", userService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<User> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }
}
