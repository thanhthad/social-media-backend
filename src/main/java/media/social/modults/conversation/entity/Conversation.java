package media.social.modults.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modults.conversation.enums.ConversationType;

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