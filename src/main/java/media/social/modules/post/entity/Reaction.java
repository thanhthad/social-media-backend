package media.social.modules.post.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.entity.User;
import java.time.LocalDateTime;
@Entity
@Table(
        name = "reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_post_reaction",
                        columnNames = {"user_id", "post_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false
    )
    private Post post;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "type",
            length = 50,
            nullable = false
    )
    private ReactionType type;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist(){

        createdAt = LocalDateTime.now();

    }

}