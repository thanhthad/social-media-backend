package media.social.modules.notification.event;

import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationEvent(
        Long receiverId,
        Long senderId,
        EntityType entityType,
        Long entityId,
        NotificationType type,
        LocalDateTime createdAt
) {
    public NotificationEvent(
            Long receiverId,
            Long senderId,
            EntityType entityType,
            Long entityId,
            NotificationType type
    ) {
        this(receiverId, senderId, entityType, entityId, type, LocalDateTime.now());
    }
}
