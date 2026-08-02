package media.social.modules.conversation.repository;

import media.social.modules.conversation.dto.response.ConversationMemberResponse;
import media.social.modules.conversation.entity.Conversation;
import media.social.modules.conversation.entity.ConversationMember;
import media.social.modules.conversation.entity.ConversationMemberId;
import media.social.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("""
        select cm.user
        from ConversationMember cm
        where cm.conversation.id = :conversationId
          and cm.user.id <> :senderId
    """)
    List<User> findOtherMembers(
            @Param("conversationId") Long conversationId,
            @Param("senderId") Long senderId
    );

    @Query("""
        SELECT cm.conversation
        FROM ConversationMember cm
        WHERE cm.user.id = :userId
        ORDER BY cm.conversation.lastMessageAt DESC
    """)
    List<Conversation> findConversationsByUserId(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT new media.social.modults.conversation.dto.response.ConversationMemberResponse(
        u.id,
        u.username,
        p.avatarUrl
    )
    FROM ConversationMember cm
    JOIN cm.user u
    LEFT JOIN u.profile p
    WHERE cm.conversation.id = :conversationId
""")
    List<ConversationMemberResponse> findMembersByConversationId(
            @Param("conversationId") Long conversationId
    );

    @Query("""
    SELECT c
    FROM Conversation c
    JOIN ConversationMember cm1
        ON cm1.conversation = c
    JOIN ConversationMember cm2
        ON cm2.conversation = c
    WHERE c.type = media.social.modults.conversation.enums.ConversationType.PRIVATE
    AND cm1.user.id = :userId
    AND cm2.user.id = :targetUserId
    AND (
        SELECT COUNT(cm3)
        FROM ConversationMember cm3
        WHERE cm3.conversation = c
    ) = 2
""")
    Optional<Conversation> findPrivateConversation(
            @Param("userId") Long userId,
            @Param("targetUserId") Long targetUserId
    );
}