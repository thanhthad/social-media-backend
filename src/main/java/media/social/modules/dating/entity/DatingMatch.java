package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.user.entity.User;

import java.time.OffsetDateTime;


@Entity
@Table(
        name = "dating_matches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name="uk_match_pair",
                        columnNames={
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
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_one_id")
    private User userOne;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_two_id")
    private User userTwo;

    private String status;

    private OffsetDateTime matchedAt;

    private OffsetDateTime lastMessageAt;
}