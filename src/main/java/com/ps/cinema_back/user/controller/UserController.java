package com.ps.cinema_back.user.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.common.response.PageResponse;
import com.ps.cinema_back.user.dto.request.UserRequest;
import com.ps.cinema_back.user.dto.response.UserResponse;
import com.ps.cinema_back.user.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController extends BaseController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody UserRequest userRequest) {
        return CREATED(userService.createUser(userRequest), "User created successfully");
    }

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(@Valid @RequestBody UserRequest userRequest) {
        return CREATED(userService.createUser(userRequest), "Staff account created successfully");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return OK(userService.getUserById(id), "User retrieved successfully");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return OK(userService.getAllUsers(), "All users fetched successfully");
    }

    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getTrashUsers() {
        return OK(userService.getTrashUsers(), "Trash users fetched successfully");
    }

    // GET /api/v1/users/page?page=0&size=10&sortBy=createdAt&direction=desc
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsersPage(
            @Parameter(hidden = true)
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageRequest = PageRequest.of(page, size, sort);

        return OK(PageResponse.of(userService.getUsersPage(pageRequest)), "Users page fetched successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable Long id, @Valid @RequestBody UserRequest userRequest) {
        return OK(userService.updateUser(id, userRequest), "User updated successfully");
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Long id) {
        return OK(userService.toggleUserStatus(id), "User status updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return OK(null, "User moved to trash successfully");
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<UserResponse>> restoreUser(@PathVariable Long id) {
        return OK(userService.restoreUser(id), "User restored successfully");
    }

    @DeleteMapping("/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return OK(null, "User permanently deleted successfully");
    }
}