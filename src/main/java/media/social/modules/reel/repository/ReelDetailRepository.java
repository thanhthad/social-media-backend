package media.social.modules.reel.repository;

import media.social.modules.reel.entity.ReelDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReelDetailRepository extends JpaRepository<ReelDetail, Long> {

    Optional<ReelDetail> findByPostId(Long postId);

    @Modifying
    @Query("""
        UPDATE ReelDetail r
        SET r.viewCount = r.viewCount + 1
        WHERE r.reelId = :reelId
    """)
    int incrementViewCount(@Param("reelId") Long reelId);

    @Modifying
    @Query("""
        UPDATE ReelDetail r
        SET r.shareCount = r.shareCount + 1
        WHERE r.reelId = :reelId
    """)
    int incrementShareCount(@Param("reelId") Long reelId);
}