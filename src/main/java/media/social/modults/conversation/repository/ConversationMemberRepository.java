package media.social.modults.conversation.repository;

import media.social.modults.conversation.dto.response.ConversationMemberResponse;
import media.social.modults.conversation.entity.ConversationMember;
import media.social.modults.conversation.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    void deleteByConversationIdAndUserId(Long conversationId, Long userId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Query("""
        SELECT new media.social.modults.conversation_member.dto.response.ConversationMemberResponse(
            u.id,
            u.username,
            p.avatarUrl
        )
        FROM ConversationMember cm
        JOIN cm.user u
        LEFT JOIN u.profile p
        WHERE cm.conversation.id = :conversationId
        """)
    List<ConversationMemberResponse> findMembersByConversationId(Long conversationId);

}