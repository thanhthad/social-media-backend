package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingSwipe;
import media.social.modules.dating.enums.DatingSwipeAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatingSwipeRepository extends JpaRepository<DatingSwipe, Long> {

    Optional<DatingSwipe> findBySwiperUserIdAndTargetUserId(Long swiperId, Long targetId);

    boolean existsBySwiperUserIdAndTargetUserId(Long swiperId, Long targetId);

    List<DatingSwipe> findAllBySwiperUserIdOrderByCreatedAtDesc(Long swiperId);

    List<DatingSwipe> findAllBySwiperUserIdAndActionOrderByCreatedAtDesc(Long swiperId, DatingSwipeAction action);

    void deleteBySwiperUserIdAndTargetUserId(Long swiperId, Long targetId);
}