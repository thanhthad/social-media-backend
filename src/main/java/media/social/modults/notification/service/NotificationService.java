package media.social.modults.notification.service;

import media.social.modults.notification.dto.response.NotificationResponse;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    void delete(
            Long receiverId,
            Long senderId,
            EntityType entityType,
            Long entityId,
            NotificationType type
    );

    void create(
            User receiver,
            User sender,
            EntityType entityType,
            Long entityId,
            NotificationType type
    );

    Page<NotificationResponse> getMyNotifications(
            Pageable pageable
    );

    void markAsRead(
            Long notificationId
    );

    void markAllAsRead();

    long countUnread();

}