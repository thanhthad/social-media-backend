package media.social.modules.conversation.repository;

import media.social.modules.conversation.dto.projection.MessageReactionUserProjection;
import media.social.modules.conversation.dto.response.MessageReactionUserResponse;
import media.social.modules.conversation.entity.MessageReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    SELECT
        u.id AS userId,
        u.username AS userName,
        p.avatarUrl AS avatarUrl,
        mr.type AS reactionType,
        mr.createdAt AS createdAt

    FROM MessageReaction mr

    JOIN mr.user u

    LEFT JOIN u.profile p

    WHERE mr.message.id = :messageId

    ORDER BY mr.createdAt DESC
""")
    List<MessageReactionUserProjection> findUsersReacted(
            @Param("messageId") Long messageId
    );
}