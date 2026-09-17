package media.social.modules.reel.repository;

import media.social.modules.reel.entity.ReelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReelViewRepository extends JpaRepository<ReelView, Long> {

    /**
     * Tìm view record theo reelId + userId.
     * Dùng để kiểm tra xem user đã xem reel chưa.
     */
    @Query("""
        SELECT rv FROM ReelView rv
        WHERE rv.reel.id = :reelId
          AND rv.user.id = :userId
    """)
    Optional<ReelView> findByReelIdAndUserId(
            @Param("reelId") Long reelId,
            @Param("userId") Long userId
    );

    /**
     * Kiểm tra record đã tồn tại chưa (tránh load entity).
     */
    @Query("""
        SELECT COUNT(rv) > 0 FROM ReelView rv
        WHERE rv.reel.id = :reelId
          AND rv.user.id = :userId
    """)
    boolean existsByReelIdAndUserId(
            @Param("reelId") Long reelId,
            @Param("userId") Long userId
    );

    /**
     * Cập nhật watchDurationMs và completed.
     * watchDurationMs chỉ tăng (GREATEST bảo đảm không giảm).
     * completed chỉ chuyển true → không reset về false (completed OR :completed).
     */
    @Modifying
    @Query(value = """
        UPDATE reel_views
        SET watch_duration_ms = GREATEST(watch_duration_ms, :durationMs),
            completed         = completed OR :completed
        WHERE reel_id = :reelId
          AND user_id = :userId
    """, nativeQuery = true)
    int updateProgress(
            @Param("reelId") Long reelId,
            @Param("userId") Long userId,
            @Param("durationMs") Long durationMs,
            @Param("completed") boolean completed
    );

    /**
     * Tăng replayCount thêm 1.
     */
    @Modifying
    @Query(value = """
        UPDATE reel_views
        SET replay_count = replay_count + 1
        WHERE reel_id = :reelId
          AND user_id = :userId
    """, nativeQuery = true)
    int incrementReplayCount(
            @Param("reelId") Long reelId,
            @Param("userId") Long userId
    );
}
