package media.social.modults.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modults.conversation.enums.ConversationType;
import media.social.modults.user.entity.User;

import java.time.OffsetDateTime;


@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private ConversationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "name")
    private String name;


    @Column(name = "avatar_url")
    private String avatarUrl;


    @Column(name = "avatar_public_id")
    private String avatarPublicId;

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