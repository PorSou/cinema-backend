package com.ps.cinema_back.notification.repository;

import com.ps.cinema_back.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Fetch all notifications ordered by creation date descending
    List<Notification> findAllByOrderByCreatedAtDesc();

    // Count how many notifications are unread
    long countByIsReadFalse();
}