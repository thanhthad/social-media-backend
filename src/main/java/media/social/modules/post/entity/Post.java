package media.social.modules.post.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "post_type",
            length = 30,
            nullable = false
    )
    @Builder.Default
    private PostType postType = PostType.POST;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Builder.Default
    @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PostMedia> medias = new ArrayList<>();

    @Builder.Default
    @OneToMany(
            mappedBy = "post",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<PostHashtag> postHashtags = new ArrayList<>();

    @Column(
            name = "comment_count",
            nullable = false
    )
    @Builder.Default
    private Long commentCount = 0L;

    @Column(
            name = "reaction_count",
            nullable = false
    )
    @Builder.Default
    private Long reactionCount = 0L;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        }

    @PreUpdate
    void preUpdate() {

        updatedAt = LocalDateTime.now();
    }
}