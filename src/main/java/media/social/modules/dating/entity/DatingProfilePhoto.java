package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dating_profile_photos"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingProfilePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "photo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "dating_profile_id",
            nullable = false
    )
    private DatingProfile datingProfile;

    @Column(
            name = "public_id",
            nullable = false,
            length = 255
    )
    private String publicId;

    @Column(
            name = "image_url",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String imageUrl;

    @Column(
            name = "display_order",
            nullable = false
    )
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(
            name = "is_primary",
            nullable = false
    )
    @Builder.Default
    private Boolean primary = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}