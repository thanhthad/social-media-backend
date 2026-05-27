package media.social.modults.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    private String username;

    private String email;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    private LocalDateTime createdAt;

    @Builder.Default
    private String role = "USER";

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}