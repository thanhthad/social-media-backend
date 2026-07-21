package media.social.modults.conversation.repository;

import media.social.modults.conversation.entity.Conversation;
import media.social.modults.conversation.enums.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

}