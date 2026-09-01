package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DatingMatchRepository extends JpaRepository<DatingMatch, Long> {

    boolean existsByUserOneIdAndUserTwoId(
            Long userOneId,
            Long userTwoId
    );

    @Query("""
    SELECT m
    FROM DatingMatch m
    JOIN FETCH m.userOne u1
    LEFT JOIN FETCH u1.profile p1
    JOIN FETCH m.userTwo u2
    LEFT JOIN FETCH u2.profile p2
    WHERE (m.userOne.id = :userId OR m.userTwo.id = :userId)
      AND m.status = media.social.modules.dating.enums.DatingMatchStatus.ACTIVE
    ORDER BY m.matchedAt DESC
    """)
    List<DatingMatch> findActiveMatchesByUserId(@Param("userId") Long userId);
}