package com.neovarsity.toursattractions.service;

import com.neovarsity.toursattractions.dto.request.LoginRequest;
import com.neovarsity.toursattractions.dto.request.RegisterRequest;
import com.neovarsity.toursattractions.dto.response.UserResponse;
import com.neovarsity.toursattractions.entity.User;
import com.neovarsity.toursattractions.entity.enums.UserRole;
import com.neovarsity.toursattractions.entity.enums.UserRole;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ForbiddenException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.toursattractions.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String SESSION_USER_ID = "LOGGED_IN_USER_ID";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.CUSTOMER)
                .build();
        return toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request, HttpSession session) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid email or password");
        }
        session.setAttribute(SESSION_USER_ID, user.getId());
        return toResponse(user);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(HttpSession session) {
        Long userId = getSessionUserId(session);
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public User requireUser(HttpSession session) {
        Long userId = getSessionUserId(session);
        if (userId == null) {
            throw new BusinessException("Please log in to continue");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public Long getSessionUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(SESSION_USER_ID);
        return id instanceof Long ? (Long) id : null;
    }

    public boolean isLoggedIn(HttpSession session) {
        return getSessionUserId(session) != null;
    }

    @Transactional(readOnly = true)
    public User requireAdmin(HttpSession session) {
        User user = requireUser(session);
        if (user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Administrator privileges required");
        }
        return user;
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .build();
    }
}
