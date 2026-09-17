package media.social.modules.reel.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.entity.Post;

import java.time.LocalDateTime;

@Entity
@Table(name = "reel_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelDetail {

    @Id
    @Column(name = "reel_id")
    private Long reelId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "reel_id",
            nullable = false
    )
    private Post post;

    @Column(
            name = "duration_seconds",
            nullable = false
    )
    private Integer durationSeconds;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @Column(name = "thumbnail_public_id", length = 255)
    private String thumbnailPublicId;

    @Column(
            name = "view_count",
            nullable = false
    )
    @Builder.Default
    private Long viewCount = 0L;

    @Column(
            name = "share_count",
            nullable = false
    )
    @Builder.Default
    private Long shareCount = 0L;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}