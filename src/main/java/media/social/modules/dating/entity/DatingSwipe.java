package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.dating.enums.DatingSwipeAction;
import media.social.modules.user.entity.User;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dating_swipes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_target_swipe",
                        columnNames = {
                                "swiper_id",
                                "target_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingSwipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "swipe_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "swiper_id",
            nullable = false
    )
    private User swiper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "target_id",
            nullable = false
    )
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private DatingSwipeAction action;

    @Column(
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {

        createdAt = OffsetDateTime.now();

    }
}