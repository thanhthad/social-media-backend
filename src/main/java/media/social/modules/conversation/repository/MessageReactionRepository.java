package media.social.modules.conversation.repository;

import media.social.modules.conversation.dto.response.MessageReactionUserResponse;
import media.social.modules.conversation.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository
        extends JpaRepository<MessageReaction, Long> {

    @Query("""
        SELECT mr
        FROM MessageReaction mr
        JOIN FETCH mr.user u
        WHERE mr.message.id IN :messageIds
    """)
    List<MessageReaction> findByMessageIds(
            List<Long> messageIds
    );

    List<MessageReaction> findAllByMessageId(
            Long messageId
    );

    Optional<MessageReaction> findByUserIdAndMessageId(
            Long userId,
            Long messageId
    );

    @Query("""
    SELECT new media.social.modules.conversation.dto.response.MessageReactionUserResponse(
        u.id,
        u.username,
        p.avatarUrl,
        mr.type,
        mr.createdAt
    )
    FROM MessageReaction mr
    JOIN mr.user u
    LEFT JOIN u.profile p
    WHERE mr.message.id = :messageId
    ORDER BY mr.createdAt DESC
""")
    List<MessageReactionUserResponse> findUsersReacted(
            Long messageId
    );
}