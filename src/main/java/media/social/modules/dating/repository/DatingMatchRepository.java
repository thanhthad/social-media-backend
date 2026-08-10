package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.enums.DatingMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatingMatchRepository extends JpaRepository<DatingMatch, Long> {

    Optional<DatingMatch> findByUserOneIdAndUserTwoId(
            Long userOneId,
            Long userTwoId
    );

    List<DatingMatch> findAllByUserOneIdOrUserTwoIdOrderByMatchedAtDesc(
            Long userOneId,
            Long userTwoId
    );

    List<DatingMatch> findAllByUserOneIdOrUserTwoIdAndStatusOrderByMatchedAtDesc(
            Long userOneId,
            Long userTwoId,
            DatingMatchStatus status
    );

    boolean existsByUserOneIdAndUserTwoId(
            Long userOneId,
            Long userTwoId
    );
}