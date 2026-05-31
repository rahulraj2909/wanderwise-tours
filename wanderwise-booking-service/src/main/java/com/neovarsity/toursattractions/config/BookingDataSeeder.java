package com.neovarsity.toursattractions.config;

import com.neovarsity.toursattractions.entity.User;
import com.neovarsity.toursattractions.entity.enums.UserRole;
import com.neovarsity.toursattractions.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "h2", "full"})
@RequiredArgsConstructor
public class BookingDataSeeder implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "demo123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String encoded = passwordEncoder.encode(DEMO_PASSWORD);
        ensureUser("customer@tours.demo", "Demo Customer", UserRole.CUSTOMER, encoded);
        ensureUser("admin@tours.demo", "Demo Admin", UserRole.ADMIN, encoded);
        System.out.println("=== Booking users seeded (customer@tours.demo / demo123) ===");
    }

    private void ensureUser(String email, String name, UserRole role, String encodedPassword) {
        userRepository.findByEmail(email.toLowerCase()).orElseGet(() -> userRepository.save(User.builder()
                .name(name)
                .email(email.toLowerCase())
                .password(encodedPassword)
                .role(role)
                .build()));
    }
}
