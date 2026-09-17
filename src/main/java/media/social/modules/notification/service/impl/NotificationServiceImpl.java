package media.social.modules.notification.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.infrastructure.kafka.producer.KafkaEventPublisher;
import media.social.modules.notification.dto.response.NotificationResponse;
import media.social.modules.notification.entity.Notification;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.event.NotificationEvent;
import media.social.modules.notification.exception.NotificationNotFoundException;
import media.social.modules.notification.repository.NotificationRepository;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.notification.websocket.NotificationPublisher;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.auth.security.context.UserContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPublisher notificationPublisher;
    private final KafkaEventPublisher kafkaEventPublisher;
    private final UserRepository userRepository;

    @Value("${app.kafka.topics.notification:social.notification.events}")
    private String notificationTopic = "social.notification.events";

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String buildMessage(NotificationType type, String senderName) {
        String name = (senderName != null && !senderName.isBlank()) ? senderName : "Một người dùng";
        return switch (type) {
            case POST_REACTION     -> name + " đã bày tỏ cảm xúc về bài viết của bạn.";
            case POST_COMMENT      -> name + " đã bình luận về bài viết của bạn.";
            case COMMENT           -> name + " đã bình luận về bài viết của bạn.";
            case COMMENT_REPLY     -> name + " đã trả lời bình luận của bạn.";
            case COMMENT_REACTION  -> name + " đã thích bình luận của bạn.";
            case FRIEND_REQUEST    -> name + " đã gửi lời mời kết bạn cho bạn.";
            case FRIEND_ACCEPTED   -> name + " đã chấp nhận lời mời kết bạn.";
            case FOLLOW            -> name + " đã bắt đầu theo dõi bạn.";
        };
    }

    private String buildTargetUrl(NotificationType type, EntityType entityType, Long entityId, Long senderId) {
        if (type == NotificationType.FRIEND_REQUEST || type == NotificationType.FRIEND_ACCEPTED) {
            return "/friends";
        }
        if (type == NotificationType.FOLLOW && senderId != null) {
            return "/users/" + senderId;
        }
        if (entityType == EntityType.POST && entityId != null) {
            return "/posts/" + entityId;
        }
        if (entityType == EntityType.COMMENT && entityId != null) {
            // Navigate to the post that contains the comment (entityId = postId for comment notifications)
            return "/posts/" + entityId;
        }
        if (senderId != null) {
            return "/users/" + senderId;
        }
        return "/";
    }

    private NotificationResponse toResponse(
            Long notificationId,
            Long senderId,
            String senderUsername,
            String senderFullName,
            String senderAvatar,
            NotificationType type,
            EntityType entityType,
            Long entityId,
            boolean isRead,
            LocalDateTime createdAt
    ) {
        String displayName = (senderFullName != null && !senderFullName.isBlank())
                ? senderFullName
                : senderUsername;

        return NotificationResponse.builder()
                .notificationId(notificationId)
                .senderId(senderId)
                .senderUsername(senderUsername)
                .senderFullName(senderFullName)
                .senderAvatar(senderAvatar)
                .type(type)
                .entityType(entityType)
                .entityId(entityId)
                .isRead(isRead)
                .createdAt(createdAt)
                .message(buildMessage(type, displayName))
                .targetUrl(buildTargetUrl(type, entityType, entityId, senderId))
                .build();
    }

    // ── Service Methods ───────────────────────────────────────────────────────

    @Override
    public void create(
            User receiver,
            User sender,
            EntityType entityType,
            Long entityId,
            NotificationType type
    ) {
        if (sender != null && receiver.getId().equals(sender.getId())) {
            return;
        }

        NotificationEvent event = new NotificationEvent(
                receiver.getId(),
                sender != null ? sender.getId() : null,
                entityType,
                entityId,
                type,
                LocalDateTime.now()
        );

        sendNotificationEvent(event);
    }

    @Override
    public void sendNotificationEvent(NotificationEvent event) {
        if (event.senderId() != null && event.senderId().equals(event.receiverId())) {
            return;
        }

        kafkaEventPublisher.publish(
                notificationTopic,
                event.receiverId().toString(),
                event
        );
    }

    @Override
    @Transactional
    public void processNotificationEvent(NotificationEvent event) {
        if (event.senderId() != null && event.senderId().equals(event.receiverId())) {
            return;
        }

        User receiver = userRepository.findById(event.receiverId()).orElse(null);
        if (receiver == null) {
            log.warn("Cannot process notification: receiver with id {} not found", event.receiverId());
            return;
        }

        User sender = event.senderId() != null
                ? userRepository.findById(event.senderId()).orElse(null)
                : null;

        saveAndPublish(receiver, sender, event.entityType(), event.entityId(), event.type());
    }

    @Override
    @Transactional
    public void saveAndPublish(
            User receiver,
            User sender,
            EntityType entityType,
            Long entityId,
            NotificationType type
    ) {
        if (sender != null && receiver.getId().equals(sender.getId())) {
            return;
        }

        Notification notification = notificationRepository
                .findByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                        receiver.getId(),
                        sender != null ? sender.getId() : null,
                        entityType,
                        entityId,
                        type
                )
                .orElseGet(() -> Notification.builder()
                        .receiver(receiver)
                        .sender(sender)
                        .entityType(entityType)
                        .entityId(entityId)
                        .type(type)
                        .build());

        notification.setCreatedAt(LocalDateTime.now());
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);

        String senderFullName = (sender != null && sender.getProfile() != null) ? sender.getProfile().getFullName() : null;
        String senderAvatar   = (sender != null && sender.getProfile() != null) ? sender.getProfile().getAvatarUrl()  : null;
        String senderUsername = sender != null ? sender.getUsername() : "Hệ thống";

        NotificationResponse response = toResponse(
                saved.getId(),
                sender != null ? sender.getId() : null,
                senderUsername,
                senderFullName,
                senderAvatar,
                saved.getType(),
                saved.getEntityType(),
                saved.getEntityId(),
                saved.getIsRead(),
                saved.getCreatedAt()
        );

        notificationPublisher.sendToUser(receiver.getId(), response);
    }

    @Override
    @Transactional
    public void delete(
            Long receiverId,
            Long senderId,
            EntityType entityType,
            Long entityId,
            NotificationType type
    ) {
        boolean exists = notificationRepository
                .existsByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                        receiverId, senderId, entityType, entityId, type);
        if (!exists) return;

        notificationRepository
                .deleteByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                        receiverId, senderId, entityType, entityId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(Pageable pageable, boolean unreadOnly) {
        Long userId = UserContextHolder.getUserId();

        var page = unreadOnly
                ? notificationRepository.findMyUnreadNotifications(userId, pageable)
                : notificationRepository.findMyNotifications(userId, pageable);

        return page.map(row -> toResponse(
                row.getNotificationId(),
                row.getSenderId(),
                row.getSenderUsername(),
                row.getSenderFullName(),
                row.getSenderAvatar(),
                row.getType(),
                row.getEntityType(),
                row.getEntityId(),
                row.getIsRead(),
                row.getCreatedAt()
        ));
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Long userId = UserContextHolder.getUserId();
        Notification notification = notificationRepository
                .findByIdAndReceiver_Id(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        notification.setIsRead(true);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        Long userId = UserContextHolder.getUserId();
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread() {
        Long userId = UserContextHolder.getUserId();
        return notificationRepository.countByReceiver_IdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void deleteById(Long notificationId) {
        Long userId = UserContextHolder.getUserId();
        Notification notification = notificationRepository
                .findByIdAndReceiver_Id(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        notificationRepository.delete(notification);
    }
}