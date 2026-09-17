package media.social.modules.story.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.enums.MediaType;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "story_media",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_story_media_story",
                        columnNames = "story_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "story_id",
            nullable = false,
            unique = true
    )
    private Story story;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(name = "public_id", nullable = false, length = 255)
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 50)
    private MediaType mediaType;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
    }
}