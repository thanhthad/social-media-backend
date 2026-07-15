package media.social.modults.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "receiver_id",
            nullable = false
    )
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "entity_type",
            nullable = false,
            length = 50
    )
    private EntityType entityType;

    @Column(
            name = "entity_id",
            nullable = false
    )
    private Long entityId;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 50
    )
    private NotificationType type;

    @Builder.Default
    @Column(
            name = "is_read",
            nullable = false
    )
    private Boolean isRead = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {

        createdAt = LocalDateTime.now();

        if (isRead == null) {
            isRead = false;
        }
    }
}