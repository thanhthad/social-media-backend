package media.social.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @Column(name = "oauth_id", unique = true, nullable = false)
//    private String oauthId;
//
//    @Column(nullable = false)
//    private String provider;

    private String username;

    private String email;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    private LocalDateTime createdAt = LocalDateTime.now();

    private List<Post> posts;

    private List<Comment> comments;
}