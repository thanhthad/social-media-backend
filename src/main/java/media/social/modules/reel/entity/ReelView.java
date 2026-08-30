package media.social.modules.reel.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.entity.Post;
import media.social.modules.user.entity.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "reel_views")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long viewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reel_id", nullable = false)
    private Post reel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "watch_duration_ms", nullable = false)
    @Builder.Default
    private Long watchDurationMs = 0L;

    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    @Column(name = "replay_count", nullable = false)
    @Builder.Default
    private Integer replayCount = 0;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
