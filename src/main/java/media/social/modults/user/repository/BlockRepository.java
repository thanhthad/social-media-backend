package media.social.modults.user.repository;

import media.social.modults.user.entity.Block;
import media.social.modults.user.entity.BlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, BlockId> {

    // check A block B
    @Query("""
        SELECT b FROM Block b
        WHERE b.blocker.id = :blockerId
        AND b.blocked.id = :blockedId
    """)
    Optional<Block> findBlock(@Param("blockerId") Long blockerId,
                              @Param("blockedId") Long blockedId);

    // list user đã block
    @Query("""
        SELECT b FROM Block b
        JOIN FETCH b.blocked u
        WHERE b.blocker.id = :userId
        ORDER BY b.createdAt DESC
    """)
    List<Block> findAllByBlockerId(@Param("userId") Long userId);
}