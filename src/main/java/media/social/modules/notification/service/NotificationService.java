package media.social.modules.notification.service;

import media.social.modules.notification.dto.response.NotificationResponse;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.user.entity.User;
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

    void sendNotificationEvent(media.social.modules.notification.event.NotificationEvent event);

    void processNotificationEvent(media.social.modules.notification.event.NotificationEvent event);

    void saveAndPublish(
            User receiver,
            User sender,
            EntityType entityType,
            Long entityId,
            NotificationType type
    );

    Page<NotificationResponse> getMyNotifications(
            Pageable pageable,
            boolean unreadOnly
    );

    void markAsRead(Long notificationId);

    void markAllAsRead();

    long countUnread();

    void deleteById(Long notificationId);
}