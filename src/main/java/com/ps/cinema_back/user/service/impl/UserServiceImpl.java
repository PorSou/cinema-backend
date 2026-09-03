package com.ps.cinema_back.user.service.impl;

import com.ps.cinema_back.common.enums.Role;
import com.ps.cinema_back.common.exception.ConflictException;
import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.user.dto.request.UserRequest;
import com.ps.cinema_back.user.dto.response.UserResponse;
import com.ps.cinema_back.user.entity.User;
import com.ps.cinema_back.user.repository.UserRepository;
import com.ps.cinema_back.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new ConflictException("Email already taken: " + request.getEmail());
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository
                .findAllByIsDeletedFalse() // 👈 Filter out deleted users from the main list
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getTrashUsers() {
        return userRepository
                .findAllByIsDeletedTrue() // 👈 Only return soft-deleted users in trash
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersPage(Pageable pageable) {
        return userRepository
                .findAllByIsDeletedFalse(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getEmail() != null && !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
                throw new ConflictException("Email already taken: " + request.getEmail());
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public UserResponse toggleUserStatus(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository
                .findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse restoreUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new ConflictException("User is not deleted");
        }

        user.setIsDeleted(false);
        User restoredUser = userRepository.save(user);
        return mapToResponse(restoredUser);
    }

    @Override
    @Transactional
    public void hardDeleteUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        userRepository.delete(user);
    }

    private UserResponse mapToResponse(User user) {
        boolean isUpdated = user.getUpdatedAt() != null
                && user.getCreatedAt() != null
                && user.getUpdatedAt().isAfter(user.getCreatedAt());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .createdAt(user.getCreatedAt())
                .updatedAt(isUpdated ? user.getUpdatedAt() : null)
                .build();
    }
}