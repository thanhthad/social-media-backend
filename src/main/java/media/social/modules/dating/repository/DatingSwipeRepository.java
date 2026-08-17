package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingSwipe;
import media.social.modules.dating.enums.DatingSwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DatingSwipeRepository extends JpaRepository<DatingSwipe, Long> {

    Optional<DatingSwipe> findBySwiperIdAndTargetId(
            Long swiperId,
            Long targetId
    );

    @Query("""
    SELECT CASE WHEN COUNT(ds) > 0 THEN true ELSE false END
    FROM DatingSwipe ds
    WHERE ds.swiper.id = :swiperId
      AND ds.target.id = :targetId
      AND ds.action = :action
""")
    boolean existsBySwiperIdAndTargetIdAndAction(
            @Param("swiperId") Long swiperId,
            @Param("targetId") Long targetId,
            @Param("action") DatingSwipeAction action
    );

    List<DatingSwipe> findAllBySwiperIdOrderByCreatedAtDesc(
            Long swiperId
    );

    List<DatingSwipe> findAllBySwiperIdAndActionOrderByCreatedAtDesc(
            Long swiperId,
            DatingSwipeAction action
    );

    void deleteBySwiperIdAndTargetId(
            Long swiperId,
            Long targetId
    );
}