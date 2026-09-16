package com.ps.cinema_back.user.service.impl;

import com.ps.cinema_back.audit.service.AuditLogService;
import com.ps.cinema_back.cloudinary.service.CloudinaryService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final CloudinaryService cloudinaryService; // 🌟 Injected Cloudinary service

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

        auditLogService.logAction(
                "CREATE_USER",
                "Created new user account: '" + savedUser.getEmail() + "' with role: " + savedUser.getRole()
        );

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
    public UserResponse getCurrentUserProfile(String email) {
        User user = userRepository
                .findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfileWithAvatar(String email, String fullName, String phone, MultipartFile file) {
        User user = userRepository
                .findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }

        // 🌟 Handle Cloudinary avatar image file upload via dialog picker
        if (file != null && !file.isEmpty()) {
            String avatarUrl = cloudinaryService.uploadImage(file);
            user.setAvatarUrl(avatarUrl);
        }

        User updatedUser = userRepository.save(user);

        auditLogService.logAction(
                "UPDATE_PROFILE",
                "User '" + updatedUser.getEmail() + "' updated their personal profile and avatar."
        );

        return mapToResponse(updatedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository
                .findAllByIsDeletedFalse()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getTrashUsers() {
        return userRepository
                .findAllByIsDeletedTrue()
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

        auditLogService.logAction(
                "UPDATE_USER",
                "Updated user account ID " + id + " (Email: " + updatedUser.getEmail() + ", Role: " + updatedUser.getRole() + ")"
        );

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

        auditLogService.logAction(
                "TOGGLE_USER_STATUS",
                "Changed active status of user '" + updatedUser.getEmail() + "' (ID: " + id + ") to active = " + updatedUser.getIsActive()
        );

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

        auditLogService.logAction(
                "SOFT_DELETE_USER",
                "Moved user account '" + user.getEmail() + "' (ID: " + id + ") to trash"
        );
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

        auditLogService.logAction(
                "RESTORE_USER",
                "Restored user account '" + restoredUser.getEmail() + "' (ID: " + id + ") from trash"
        );

        return mapToResponse(restoredUser);
    }

    @Override
    @Transactional
    public void hardDeleteUser(Long id) {
        User user = userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String userEmail = user.getEmail();
        userRepository.delete(user);

        auditLogService.logAction(
                "HARD_DELETE_USER",
                "Permanently deleted user account '" + userEmail + "' (ID: " + id + ")"
        );
    }

    private UserResponse mapToResponse(User user) {
        boolean isUpdated = user.getUpdatedAt() != null
                && user.getCreatedAt() != null
                && user.getUpdatedAt().isAfter(user.getCreatedAt());

        // Resolve full Cloudinary URL or fallback safely
        String fullAvatarUrl = null;
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
            if (user.getAvatarUrl().startsWith("http://") || user.getAvatarUrl().startsWith("https://")) {
                fullAvatarUrl = user.getAvatarUrl();
            } else {
                try {
                    fullAvatarUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/")
                            .path(user.getAvatarUrl().startsWith("/") ? user.getAvatarUrl().substring(1) : user.getAvatarUrl())
                            .toUriString();
                } catch (Exception e) {
                    fullAvatarUrl = user.getAvatarUrl();
                }
            }
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(fullAvatarUrl)
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isDeleted(user.getIsDeleted())
                .createdAt(user.getCreatedAt())
                .updatedAt(isUpdated ? user.getUpdatedAt() : null)
                .build();
    }
}