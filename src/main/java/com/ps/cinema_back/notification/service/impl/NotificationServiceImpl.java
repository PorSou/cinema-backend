package com.ps.cinema_back.notification.service.impl;

import com.ps.cinema_back.common.exception.ResourceNotFoundException;
import com.ps.cinema_back.notification.entity.Notification;
import com.ps.cinema_back.notification.repository.NotificationRepository;
import com.ps.cinema_back.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByIsReadFalse();
    }

    @Override
    @Transactional
    public Notification markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        List<Notification> unreadList = notificationRepository.findAll()
                .stream()
                .filter(n -> !n.getIsRead())
                .toList();

        unreadList.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadList);
    }

    @Override
    @Transactional
    public void createNotification(String title, String message, String type) {
        Notification notification = Notification.builder()
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }
}