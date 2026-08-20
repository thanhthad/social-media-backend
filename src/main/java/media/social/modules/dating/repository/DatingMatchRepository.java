package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatingMatchRepository extends JpaRepository<DatingMatch, Long> {

    boolean existsByUserOneIdAndUserTwoId(
            Long userOneId,
            Long userTwoId
    );

}