package media.social.modules.dating.repository;

import media.social.modules.auth.Enum.Status;
import media.social.modules.dating.dto.projection.DatingConversationListProjection;
import media.social.modules.dating.entity.DatingMatch;
import media.social.modules.dating.enums.DatingMatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
    SELECT
        dm.conversation.id AS conversationId,
        dm.id AS matchId,

        CASE
            WHEN dm.userOne.id = :userId
                THEN dm.userTwo.id
            ELSE dm.userOne.id
        END AS userId,

        CASE
            WHEN dm.userOne.id = :userId
                THEN profileTwo.displayName
            ELSE profileOne.displayName
        END AS displayName,

        CASE
            WHEN dm.userOne.id = :userId
                THEN photoTwo.imageUrl
            ELSE photoOne.imageUrl
        END AS avatarUrl,

        dm.matchedAt AS matchedAt,
        dm.lastMessageAt AS lastMessageAt

    FROM DatingMatch dm

    JOIN DatingProfile profileOne
        ON profileOne.user.id = dm.userOne.id

    JOIN DatingProfile profileTwo
        ON profileTwo.user.id = dm.userTwo.id

    LEFT JOIN DatingProfilePhoto photoOne
        ON photoOne.datingProfile.id = profileOne.id
        AND photoOne.primary = true

    LEFT JOIN DatingProfilePhoto photoTwo
        ON photoTwo.datingProfile.id = profileTwo.id
        AND photoTwo.primary = true

    WHERE
        (
            dm.userOne.id = :userId
            OR dm.userTwo.id = :userId
        )

        AND (
            CASE
                WHEN dm.userOne.id = :userId
                    THEN dm.userTwo.status
                ELSE dm.userOne.status
            END
        ) <> :bannedStatus

        AND NOT EXISTS (
            SELECT 1
            FROM DatingReport r
            WHERE
                (
                    r.reporter.id = :userId
                    AND r.reportedUser.id =
                        CASE
                            WHEN dm.userOne.id = :userId
                                THEN dm.userTwo.id
                            ELSE dm.userOne.id
                        END
                )
                OR
                (
                    r.reporter.id =
                        CASE
                            WHEN dm.userOne.id = :userId
                                THEN dm.userTwo.id
                            ELSE dm.userOne.id
                        END
                    AND r.reportedUser.id = :userId
                )
        )

    ORDER BY
        dm.lastMessageAt DESC NULLS LAST,
        dm.matchedAt DESC
""")
    List<DatingConversationListProjection> findMyDatingConversations(
            @Param("userId") Long userId,
            @Param("bannedStatus") Status bannedStatus
    );
}