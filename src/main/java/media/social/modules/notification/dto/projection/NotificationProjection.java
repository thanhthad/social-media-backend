package media.social.modules.notification.dto.projection;

import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;

import java.time.LocalDateTime;

public interface NotificationProjection {

    Long getNotificationId();

    Long getSenderId();

    String getSenderUsername();

    String getSenderFullName();

    String getSenderAvatar();

    NotificationType getType();

    EntityType getEntityType();

    Long getEntityId();

    boolean getIsRead();

    LocalDateTime getCreatedAt();
}