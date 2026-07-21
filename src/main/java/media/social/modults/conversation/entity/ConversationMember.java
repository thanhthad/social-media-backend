package media.social.modults.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import media.social.modults.user.entity.User;

import java.time.OffsetDateTime;

@Entity
@Table(name = "conversation_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMember {


    @EmbeddedId
    private ConversationMemberId id;


    @MapsId("conversationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "conversation_id",
            nullable = false
    )
    private Conversation conversation;


    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    @Column(
            name = "joined_at",
            nullable = false,
            updatable = false
    )
    private OffsetDateTime joinedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_read_message_id")
    private Message lastReadMessage;


    @PrePersist
    public void prePersist() {
        this.joinedAt = OffsetDateTime.now();
    }
}