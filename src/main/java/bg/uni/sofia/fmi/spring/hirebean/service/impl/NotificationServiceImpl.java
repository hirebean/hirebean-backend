package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.NotificationResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.ResourceNotFoundException;
import bg.uni.sofia.fmi.spring.hirebean.model.entity.Notification;
import bg.uni.sofia.fmi.spring.hirebean.repository.NotificationRepository;
import bg.uni.sofia.fmi.spring.hirebean.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientId(notification.getRecipient().getId())
                .message(notification.getMessage())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUser(Long userId) {
        return notificationRepository.findAllByRecipientId(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {

        List<Notification> notifications = notificationRepository.findAllByRecipientIdAndIsRead(userId, false);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotifications(Long userId) {
        return notificationRepository.countByRecipientIdAndIsRead(userId, false);
    }
}
