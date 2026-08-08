package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.dating.enums.DatingMatchStatus;
import media.social.modules.user.entity.User;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dating_matches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_match_pair",
                        columnNames = {
                                "user_one_id",
                                "user_two_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_one_id",
            nullable = false
    )
    private User userOne;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_two_id",
            nullable = false
    )
    private User userTwo;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private DatingMatchStatus status;

    @Column(
            nullable = false,
            updatable = false
    )
    private OffsetDateTime matchedAt;

    private OffsetDateTime lastMessageAt;

    @PrePersist
    public void prePersist() {
        matchedAt = OffsetDateTime.now();
        if (status == null) {
            status = DatingMatchStatus.ACTIVE;
        }
    }
}