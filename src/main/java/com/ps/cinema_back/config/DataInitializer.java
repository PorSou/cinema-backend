package com.ps.cinema_back.config;

import com.ps.cinema_back.common.enums.Role;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initDefaultUsers() {
        return args -> {
            // 1. Seed Default Super Admin
            String adminEmail = "admin@cinema.com";
            if (!userRepository.existsByEmailAndIsDeletedFalse(adminEmail)) {
                User admin = User.builder()
                        .fullName("Super Admin")
                        .email(adminEmail)
                        .phone("0123456789")
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .isActive(true)
                        .build();

                userRepository.save(admin);
                log.info("🔑 Default ADMIN created: {} / admin123", adminEmail);
            }

            // 2. Seed Default Cinema Staff (Optional)
            String staffEmail = "staff@cinema.com";
            if (!userRepository.existsByEmailAndIsDeletedFalse(staffEmail)) {
                User staff = User.builder()
                        .fullName("Cinema Staff")
                        .email(staffEmail)
                        .phone("0987654321")
                        .password(passwordEncoder.encode("staff123"))
                        .role(Role.STAFF)
                        .isActive(true)
                        .build();

                userRepository.save(staff);
                log.info("🎟️ Default STAFF created: {} / staff123", staffEmail);
            }
        };
    }
}