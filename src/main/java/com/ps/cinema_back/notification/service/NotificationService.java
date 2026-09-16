package com.ps.cinema_back.notification.service;

import com.ps.cinema_back.notification.entity.Notification;
import java.util.List;

public interface NotificationService {
    List<Notification> getAllNotifications();
    long getUnreadCount();
    Notification markAsRead(Long id);
    void markAllAsRead();
    void createNotification(String title, String message, String type);
}