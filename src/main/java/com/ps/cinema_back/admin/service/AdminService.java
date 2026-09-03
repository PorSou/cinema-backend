package com.ps.cinema_back.admin.service;
import com.ps.cinema_back.admin.dto.request.CreateStaffRequest;
import com.ps.cinema_back.user.dto.response.UserResponse;

public interface AdminService {

    UserResponse createStaff(CreateStaffRequest request);
}
