package media.social.modules.conversation.repository;
import media.social.modules.conversation.dto.projection.MessageProjection;
import media.social.modules.conversation.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findTopByConversationIdAndIdLessThanAndDeletedFalseOrderByIdDesc(
            Long conversationId,
            Long messageId
    );

    @Query("""
    SELECT m
    FROM Message m
    JOIN FETCH m.sender
    LEFT JOIN FETCH m.media
    WHERE m.id = :messageId
""")
    Optional<Message> findByIdWithSenderAndMedia(
            Long messageId
    );

    @Query("""
    SELECT COUNT(m)
    FROM Message m
    JOIN ConversationMember cm
        ON cm.conversation.id = m.conversation.id
    WHERE cm.conversation.id = :conversationId
    AND cm.user.id = :userId
    AND m.sender.id <> :userId
    AND m.deleted = false
    AND (
        cm.lastReadMessage IS NULL
        OR m.id > cm.lastReadMessage.id
    )
""")
    long countUnreadMessages(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId
    );

    @Query("""
    SELECT
        m.id AS id,
        c.id AS conversationId,
        u.id AS senderId,
        u.username AS senderName,
        p.avatarUrl AS avatarUrl,
        m.content AS content,
        rm.id AS replyToMessageId,
        m.createdAt AS createdAt
    
    FROM Message m
    JOIN m.conversation c
    JOIN m.sender u
    LEFT JOIN u.profile p
    LEFT JOIN m.replyToMessage rm
    WHERE c.id = :conversationId
    ORDER BY m.createdAt DESC
    """)
    Page<MessageProjection> findMessages(
            Long conversationId,
            Pageable pageable
    );

    boolean existsByIdAndConversationId(
            Long messageId,
            Long conversationId
    );
}