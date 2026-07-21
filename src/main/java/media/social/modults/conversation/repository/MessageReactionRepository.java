package media.social.modults.conversation.repository;

import media.social.modults.conversation.dto.response.MessageReactionUserResponse;
import media.social.modults.conversation.entity.MessageReaction;
import media.social.modults.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository
        extends JpaRepository<MessageReaction, Long> {


    Optional<MessageReaction> findByUserIdAndMessageId(
            Long userId,
            Long messageId
    );


    boolean existsByUserIdAndMessageId(
            Long userId,
            Long messageId
    );


    void deleteByUserIdAndMessageId(
            Long userId,
            Long messageId
    );


    @Query("""
        SELECT mr.type, COUNT(mr)
        FROM MessageReaction mr
        WHERE mr.message.id = :messageId
        GROUP BY mr.type
    """)
    List<Object[]> countReactionsByMessageId(
            @Param("messageId") Long messageId
    );


    @Query("""
        SELECT new media.social.modults.conversation.dto.response.MessageReactionUserResponse(
            u.id,
            u.email,
            p.avatarUrl,
            mr.createdAt
        )
        FROM MessageReaction mr
        JOIN mr.user u
        JOIN u.profile p
        WHERE mr.message.id = :messageId
        AND (:type IS NULL OR mr.type = :type)
    """)
    Page<MessageReactionUserResponse> findUsersReacted(
            @Param("messageId") Long messageId,
            @Param("type") ReactionType type,
            Pageable pageable
    );
}