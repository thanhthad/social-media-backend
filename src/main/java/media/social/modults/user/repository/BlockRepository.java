package media.social.modults.user.repository;

import media.social.modults.user.dto.response.block.ListUserBlockedResponse;
import media.social.modults.user.entity.Block;
import media.social.modults.user.entity.BlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
        SELECT b FROM Block b
        WHERE b.blocker.id = :blockerId
        AND b.blocked.id = :blockedId
    """)
    Optional<Block> findBlock(@Param("blockerId") Long blockerId,
                              @Param("blockedId") Long blockedId);

    @Query("""
    SELECT new media.social.modults.user.dto.response.block.ListUserBlockedResponse(
        u.id,
        u.username,
        p.fullName,
        p.avatarUrl
    )
    FROM Block b
    JOIN b.blocked u
    LEFT JOIN u.profile p
    WHERE b.blocker.id = :userId
    ORDER BY b.createdAt DESC
""")
    List<ListUserBlockedResponse> findBlockedUsers(@Param("userId") Long userId);
}