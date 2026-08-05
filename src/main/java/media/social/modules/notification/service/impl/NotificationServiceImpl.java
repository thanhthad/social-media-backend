package media.social.modules.notification.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.notification.dto.response.NotificationResponse;
import media.social.modules.notification.entity.Notification;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.exception.NotificationNotFoundException;
import media.social.modules.notification.repository.NotificationRepository;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.notification.websocket.NotificationPublisher;
import media.social.modules.user.entity.User;
import media.social.modules.auth.security.context.UserContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public void create(
            User receiver,
            User sender,
            EntityType entityType,
            Long entityId,
            NotificationType type
    ) {
        if (sender != null
                && receiver.getId().equals(sender.getId())) {
            return;
        }
        Notification notification = notificationRepository
                .findByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                        receiver.getId(),
                        sender.getId(),
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

        Notification saved =
                notificationRepository.save(notification);

        NotificationResponse response =
                NotificationResponse.builder()
                        .notificationId(saved.getId())
                        .senderId(sender.getId())
                        .senderUsername(sender.getUsername())
                        .senderAvatar(sender.getProfile().getAvatarUrl())
                        .type(saved.getType())
                        .entityType(saved.getEntityType())
                        .entityId(saved.getEntityId())
                        .isRead(saved.getIsRead())
                        .createdAt(saved.getCreatedAt())
                        .build();

        notificationPublisher.sendToUser(
                receiver.getId(),
                response
        );
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
                        receiverId,
                        senderId,
                        entityType,
                        entityId,
                        type
                );
        if (!exists) {
            return;
        }
        notificationRepository
                .deleteByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
                        receiverId,
                        senderId,
                        entityType,
                        entityId,
                        type
                );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(
            Pageable pageable
    ) {

        Long userId = UserContextHolder.getUserId();

        return notificationRepository.findMyNotifications(
                userId,
                pageable
        );
    }

    @Override
    @Transactional
    public void markAsRead(
            Long notificationId
    ) {

        Long userId = UserContextHolder.getUserId();

        Notification notification = notificationRepository
                .findByIdAndReceiver_Id(
                        notificationId,
                        userId
                )
                .orElseThrow(() ->
                        new NotificationNotFoundException(
                                "Notification not found"
                        )
                );

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

        return notificationRepository.countByReceiver_IdAndIsReadFalse(
                userId
        );
    }
}