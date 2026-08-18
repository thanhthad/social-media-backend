package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.dating.enums.GenderPreference;
import media.social.modules.user.entity.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "dating_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    private Integer minAge;

    private Integer maxAge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GenderPreference genderPreference;

    private Integer maxDistance;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = OffsetDateTime.now();
    }
}