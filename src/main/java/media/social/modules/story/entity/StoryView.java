package media.social.modules.story.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "story_views"
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryView {

    @EmbeddedId
    private StoryViewId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("storyId")
    @JoinColumn(
            name = "story_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_story_view_story")
    )
    private Story story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("viewerId")
    @JoinColumn(
            name = "viewer_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_story_viewer")
    )
    private User viewer;

    @Column(name = "viewed_at", nullable = false, updatable = false)
    private LocalDateTime viewedAt;

    @PrePersist
    void prePersist() {
        viewedAt = LocalDateTime.now();
    }
}