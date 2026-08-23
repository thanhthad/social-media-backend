package media.social.modules.conversation.repository;

import media.social.modules.conversation.dto.projection.ConversationListProjection;
import media.social.modules.conversation.dto.projection.DatingConversationListProjection;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {
    @Query("""
    SELECT c
    FROM Conversation c
    LEFT JOIN FETCH c.owner
    LEFT JOIN FETCH c.lastMessage
    WHERE c.id = :conversationId
""")
    Optional<Conversation> findByIdWithOwner(
            Long conversationId
    );

    @Query("""
    SELECT
        c.id AS conversationId,
        target.id,
        c.type AS type,

        CASE
            WHEN c.type = :privateType
            THEN pr.fullName
            ELSE c.name
        END AS displayName,

        CASE
            WHEN c.type = :privateType
            THEN pr.avatarUrl
            ELSE c.avatarUrl
        END AS avatarUrl,

        CASE
            WHEN m.content IS NULL
                 AND COUNT(mm.id) > 0
            THEN CONCAT(COUNT(mm.id), ' media')

            WHEN m.content IS NOT NULL
                 AND COUNT(mm.id) > 0
            THEN CONCAT(
                m.content,
                ' + ',
                COUNT(mm.id),
                ' media'
            )

            ELSE m.content
        END AS preview,

        c.lastMessageAt AS lastMessageAt

    FROM Conversation c

    LEFT JOIN c.lastMessage m

    LEFT JOIN m.media mm

    JOIN ConversationMember me
        ON me.conversation.id = c.id
       AND me.user.id = :currentUserId

    JOIN ConversationMember targetMember
        ON targetMember.conversation.id = c.id
       AND targetMember.user.id <> :currentUserId

    JOIN targetMember.user target

    JOIN target.profile pr

    WHERE c.type <> :excludedType

    GROUP BY
        c.id,
        target.id,
        c.type,
        c.name,
        c.avatarUrl,
        c.lastMessageAt,
        m.id,
        m.content,
        pr.fullName,
        pr.avatarUrl

    ORDER BY c.lastMessageAt DESC
""")
    List<ConversationListProjection> findConversationList(
            @Param("currentUserId") Long currentUserId,
            @Param("privateType") ConversationType privateType,
            @Param("excludedType") ConversationType excludedType
    );

    @Query("""
    SELECT
        c.id AS conversationId,
        pr.id AS targetUserId,
        c.type AS type,
        pr.displayName AS displayName,
        photo.imageUrl AS avatarUrl,

        CASE
            WHEN m.content IS NULL
                 AND COUNT(mm.id) > 0
            THEN CONCAT(
                COUNT(mm.id),
                ' media'
            )

            WHEN m.content IS NOT NULL
                 AND COUNT(mm.id) > 0
            THEN CONCAT(
                m.content,
                ' + ',
                COUNT(mm.id),
                ' media'
            )

            ELSE m.content
        END AS preview,

        c.lastMessageAt AS lastMessageAt

    FROM Conversation c

    LEFT JOIN c.lastMessage m

    LEFT JOIN m.media mm

    JOIN ConversationMember me
        ON me.conversation.id = c.id
       AND me.user.id = :currentUserId

    JOIN ConversationMember targetMember
        ON targetMember.conversation.id = c.id
       AND targetMember.user.id <> :currentUserId

    JOIN targetMember.user target

    JOIN target.datingProfile pr

    LEFT JOIN DatingProfilePhoto photo
        ON photo.datingProfile.id = pr.id
       AND photo.primary = true

    WHERE c.type = :conversationType

    GROUP BY
        c.id,
        c.type,
        c.lastMessageAt,
        m.id,
        m.content,
        target.id,
        pr.displayName,
        photo.imageUrl

    ORDER BY c.lastMessageAt DESC
""")
    List<DatingConversationListProjection> findDatingConversationList(
            @Param("currentUserId") Long currentUserId,
            @Param("conversationType") ConversationType conversationType
    );


}