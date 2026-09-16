package com.ps.cinema_back.notification.controller;

import com.ps.cinema_back.common.controller.BaseController;
import com.ps.cinema_back.common.response.ApiResponse;
import com.ps.cinema_back.notification.entity.Notification;
import com.ps.cinema_back.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Management", description = "Endpoints for fetching and managing admin alerts and notifications")
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all system notifications")
    public ResponseEntity<ApiResponse<List<Notification>>> getAllNotifications() {
        return OK(notificationService.getAllNotifications(), "Notifications fetched successfully");
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        return OK(notificationService.getUnreadCount(), "Unread count fetched successfully");
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<ApiResponse<Notification>> markAsRead(@PathVariable Long id) {
        return OK(notificationService.markAsRead(id), "Notification marked as read");
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        notificationService.markAllAsRead();
        return OK(null, "All notifications marked as read");
    }
}