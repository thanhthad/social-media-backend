package media.social.modults.conversation.entity;
import jakarta.persistence.*;
import lombok.*;
import media.social.modults.post.enums.MediaType;

import java.time.OffsetDateTime;
@Entity
@Table(name = "message_media")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageMedia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "message_id",
            nullable = false
    )
    private Message message;

    @Column(length = 255)
    private String url;

    @Column(name = "public_id")
    private String publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type")
    private MediaType mediaType;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = OffsetDateTime.now();
    }
}