package media.social.modults.notification.repository;

import media.social.modults.notification.dto.response.NotificationResponse;
import media.social.modults.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT new media.social.modults.notification.dto.response.NotificationResponse(
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