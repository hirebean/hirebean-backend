package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.NotificationResponse;
import java.util.List;

public interface NotificationService {

    List<NotificationResponse> getNotificationsByUser(Long userId);

    NotificationResponse markAsRead(Long notificationId);

    void markAllAsRead(Long userId);

    long countUnreadNotifications(Long userId);
}
