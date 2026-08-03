package media.social.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "email_verification_tokens",
        indexes = {
                @Index(
                        name = "idx_email_verification_token",
                        columnList = "token"
                ),
                @Index(
                        name = "idx_email_verification_user",
                        columnList = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 255
    )
    private String token;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;

    @Column(
            name = "expires_at",
            nullable = false
    )
    private LocalDateTime expiresAt;

    @Column(
            nullable = false
    )
    private Boolean used = false;

    @Column(
            name = "created_at",
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();

        if(used == null){
            used = false;
        }
    }
}