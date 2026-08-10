package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingSwipe;
import media.social.modules.dating.enums.DatingSwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatingSwipeRepository extends JpaRepository<DatingSwipe, Long> {

    Optional<DatingSwipe> findBySwiperIdAndTargetId(
            Long swiperId,
            Long targetId
    );

    boolean existsBySwiperIdAndTargetId(
            Long swiperId,
            Long targetId
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