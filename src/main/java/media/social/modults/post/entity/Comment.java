package media.social.modults.post.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modults.user.entity.User;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Builder.Default
    @OneToMany(
            mappedBy = "parent",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Comment> replies = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

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