package com.ps.cinema_back.admin.controller;


import com.ps.cinema_back.admin.dto.request.CreateStaffRequest;
import com.ps.cinema_back.admin.service.AdminService;
import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.user.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController extends BaseController {

    private final AdminService adminService;

    @PostMapping("/staff")
    public ResponseEntity<ApiResponse<UserResponse>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return CREATED(adminService.createStaff(request), "Staff account created successfully");
    }
}