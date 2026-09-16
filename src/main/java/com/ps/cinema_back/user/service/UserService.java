package com.ps.cinema_back.user.service;

import com.ps.cinema_back.user.dto.request.UserRequest;
import com.ps.cinema_back.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserRequest request);

    UserResponse getUserById(Long id);

    // 🌟 Added methods for profile management & file avatar uploads
    UserResponse getCurrentUserProfile(String email);

    UserResponse updateProfileWithAvatar(String email, String fullName, String phone, MultipartFile file);

    List<UserResponse> getAllUsers();

    List<UserResponse> getTrashUsers();

    Page<UserResponse> getUsersPage(Pageable pageable);

    UserResponse updateUser(Long id, UserRequest request);

    UserResponse toggleUserStatus(Long id);

    void deleteUser(Long id); // Soft delete

    UserResponse restoreUser(Long id);

    void hardDeleteUser(Long id); // Permanent delete
}