package media.social.modults.notification.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.notification.dto.response.NotificationResponse;
import media.social.modults.notification.entity.Notification;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.notification.exception.NotificationNotFoundException;
import media.social.modults.notification.repository.NotificationRepository;
import media.social.modults.notification.service.NotificationService;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void create(
            User receiver,
            User sender,
            EntityType entityType,
            Long entityId,
            NotificationType type
    ) {

        if (receiver.getId().equals(sender.getId())) {
            return;
        }

        Notification notification = Notification.builder()
                .receiver(receiver)
                .sender(sender)
                .entityType(entityType)
                .entityId(entityId)
                .type(type)
                .build();

        notificationRepository.save(notification);
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