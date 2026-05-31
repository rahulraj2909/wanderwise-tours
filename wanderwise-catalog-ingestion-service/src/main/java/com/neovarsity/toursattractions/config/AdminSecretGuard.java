package com.neovarsity.toursattractions.config;

import com.neovarsity.toursattractions.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AdminSecretGuard {

    @Value("${wanderwise.admin-secret:wanderwise-admin}")
    private String adminSecret;

    public void requireAdmin(String headerValue) {
        if (headerValue == null || !adminSecret.equals(headerValue)) {
            throw new BusinessException("Admin secret required (header X-Admin-Secret)");
        }
    }
}
