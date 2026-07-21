package media.social.modults.conversation.entity;
import jakarta.persistence.*;
import lombok.*;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.user.entity.User;
import java.time.OffsetDateTime;
@Entity
@Table(
        name = "message_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_message_reaction",
                        columnNames = {
                                "user_id",
                                "message_id"
                        }
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            nullable = false
    )
    private Message message;

    @Column(
            nullable = false,
            length = 50
    )
    private ReactionType type;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist(){

        this.createdAt = OffsetDateTime.now();

    }
}