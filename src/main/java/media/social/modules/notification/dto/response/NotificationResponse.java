package media.social.modules.notification.dto.response;

import lombok.*;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long notificationId;

    private Long senderId;

    private String senderUsername;

    private String senderFullName;

    private String senderAvatar;

    private NotificationType type;

    private EntityType entityType;

    private Long entityId;

    private boolean isRead;

    private LocalDateTime createdAt;

    /** Human-readable message built server-side, e.g. "Minh đã thích bài viết của bạn." */
    private String message;

    /** Client-side navigation target, e.g. "/posts/42" or "/friends" */
    private String targetUrl;
}