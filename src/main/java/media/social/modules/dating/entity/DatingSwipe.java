package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;
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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swiper_id")
    private User swiper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private User target;

    private String action;

    private OffsetDateTime createdAt;
}