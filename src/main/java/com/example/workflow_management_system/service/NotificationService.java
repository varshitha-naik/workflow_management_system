package com.example.workflow_management_system.service;

import com.example.workflow_management_system.model.Notification;
import com.example.workflow_management_system.model.Request;
import com.example.workflow_management_system.model.User;
import com.example.workflow_management_system.repository.NotificationRepository;
import com.example.workflow_management_system.repository.UserRepository;
import com.example.workflow_management_system.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    public void createNotification(String message, String type, Request request, User targetUser) {
        if (targetUser == null)
            return;
        Notification notification = new Notification(targetUser, message, type, request, request.getTenantId());
        notificationRepository.save(notification);
    }

    public void createNotificationsForRole(String message, String type, Request request, String roleName) {
        try {
            com.example.workflow_management_system.model.UserRole role = com.example.workflow_management_system.model.UserRole
                    .valueOf(roleName);
            List<User> users = userRepository.findByTenantIdAndRole(request.getTenantId(), role);
            for (User user : users) {
                createNotification(message, type, request, user);
            }
        } catch (IllegalArgumentException e) {
            // Ignore invalid roles
        }
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyRecentNotifications(int limit) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public long getMyUnreadCount() {
        Long userId = SecurityUtils.getCurrentUser().getId();
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long notificationId) {
        Long userId = SecurityUtils.getCurrentUser().getId();
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser().getId().equals(userId)) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
    }
}
