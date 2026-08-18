package media.social.modules.story.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "story_reactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_story_user_reaction",
                        columnNames = {"story_id", "user_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryReaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "story_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_story_reaction_story")
    )
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_story_reaction_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReactionType type;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}