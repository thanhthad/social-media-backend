package media.social.modules.post.repository;

import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.entity.Reaction;
import media.social.modules.post.enums.ReactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReactionRepository
        extends JpaRepository<Reaction, Long> {

    @Query("""
SELECT r
FROM Reaction r
WHERE r.user.id = :userId
AND r.post.id IN :postIds
""")
    List<Reaction> findMyReactions(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds
    );

    Optional<Reaction> findByUserIdAndPostId(
            Long userId,
            Long postId
    );

    @Query("""
    SELECT r.type, COUNT(r)
    FROM Reaction r
    WHERE r.post.id = :postId
    GROUP BY r.type
""")
    List<Object[]> countReactionTypesByPostId(
            @Param("postId") Long postId
    );

    @Query("""
        SELECT new media.social.modults.post.dto.response.reaction.UserReactionResponse(
            u.id,
            u.email,
            p.avatarUrl,
            r.createdAt
        )
        FROM Reaction r
        JOIN r.user u
        JOIN u.profile p
        WHERE r.post.id = :postId
        AND (:type IS NULL OR r.type = :type)
        """)
    Page<UserReactionResponse> findUsersReacted(
            @Param("postId") Long postId,
            @Param("type") ReactionType type,
            Pageable pageable
    );
}