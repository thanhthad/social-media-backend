package media.social.modults.conversation.repository;
import media.social.modults.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ConversationRepository
        extends JpaRepository<Conversation, Long> {

}