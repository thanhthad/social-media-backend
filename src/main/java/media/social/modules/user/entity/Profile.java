package media.social.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(
        name = "profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_profile_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "avatar_public_id")
    private String avatarPublicId;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "cover_public_id")
    private String coverPublicId;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String website;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;


    @Column(length = 100)
    private String occupation;

    @Column(length = 100)
    private String company;

    @Column(length = 150)
    private String education;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(
            name = "social_links",
            columnDefinition = "jsonb"
    )
    private Map<String, String> socialLinks;

    @Enumerated(EnumType.STRING)
    @Column( name = "profile_visibility",
            length = 20,
            nullable = false)
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Column(
            name = "created_at",
            updatable = false
    )
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();

        if(this.socialLinks == null){
            this.socialLinks = Map.of();
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}