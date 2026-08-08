package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.enums.DatingMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatingMatchRepository extends JpaRepository<DatingMatch, Long> {

    Optional<DatingMatch> findByUserOneUserIdAndUserTwoUserId(
            Long userOneId,
            Long userTwoId
    );

    List<DatingMatch> findAllByUserOneUserIdOrUserTwoUserIdOrderByMatchedAtDesc(
            Long userOneId,
            Long userTwoId
    );

    List<DatingMatch> findAllByUserOneUserIdOrUserTwoUserIdAndStatusOrderByMatchedAtDesc(
            Long userOneId,
            Long userTwoId,
            DatingMatchStatus status
    );

    boolean existsByUserOneUserIdAndUserTwoUserId(
            Long userOneId,
            Long userTwoId
    );
}