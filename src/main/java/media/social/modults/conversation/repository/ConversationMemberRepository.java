package media.social.modults.conversation.repository;

import media.social.modults.conversation.entity.ConversationMember;
import media.social.modults.conversation.entity.ConversationMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, ConversationMemberId> {

}