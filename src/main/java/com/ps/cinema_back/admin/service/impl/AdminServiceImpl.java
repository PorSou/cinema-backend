package com.ps.cinema_back.admin.service.impl;


import com.ps.cinema_back.admin.dto.request.CreateStaffRequest;
import com.ps.cinema_back.admin.service.AdminService;
import com.ps.cinema_back.common.enums.Role;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.user.dto.response.UserResponse;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ConflictException("Email is already registered: " + request.getEmail());
        }

        User staffUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF) // 👈 Explicitly assigns STAFF role
                .isActive(true)   // 👈 Staff accounts are active immediately when created by Admin
                .build();

        User savedUser = userRepository.save(staffUser);

        return mapToUserResponse(savedUser);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}