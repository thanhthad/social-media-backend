package media.social.modules.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modules.user.entity.User;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "conversation_id",
            nullable = false
    )
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id",
            nullable = false
    )
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reply_to_message_id"
    )
    private Message replyToMessage;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(
            name = "is_deleted",
            nullable = false
    )
    @Builder.Default
    private Boolean deleted = false;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime createdAt;

    @OneToMany(
            mappedBy = "message",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<MessageMedia> media =
            new ArrayList<>();

    @OneToMany(
            mappedBy = "message",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<MessageReaction> reactions =
            new ArrayList<>();

    @PrePersist
    public void prePersist(){
        this.createdAt = OffsetDateTime.now();
    }
}