package media.social.modules.notification.repository;

import media.social.modules.notification.dto.response.NotificationResponse;
import media.social.modules.notification.entity.Notification;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    void deleteByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
            Long receiverId,
            Long senderId,
            EntityType entityType,
            Long entityId,
            NotificationType type
    );

    boolean existsByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
            Long receiverId,
            Long senderId,
            EntityType entityType,
            Long entityId,
            NotificationType type
    );

    Optional<Notification> findByReceiver_IdAndSender_IdAndEntityTypeAndEntityIdAndType(
            Long receiverId,
            Long senderId,
            EntityType entityType,
            Long entityId,
            NotificationType type
    );

    @Query("""
        SELECT new media.social.modules.notification.dto.response.NotificationResponse(
            n.id,
            s.id,
            s.username,
            p.avatarUrl,
            n.type,
            n.entityType,
            n.entityId,
            n.isRead,
            n.createdAt
        )
        FROM Notification n
        JOIN n.sender s
        LEFT JOIN s.profile p
        WHERE n.receiver.id = :receiverId
        ORDER BY n.createdAt DESC
    """)
    Page<NotificationResponse> findMyNotifications(
            Long receiverId,
            Pageable pageable
    );

    Optional<Notification> findByIdAndReceiver_Id(
            Long notificationId,
            Long receiverId
    );

    long countByReceiver_IdAndIsReadFalse(
            Long receiverId
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.receiver.id = :receiverId
          AND n.isRead = false
    """)
    void markAllAsRead(Long receiverId);

}