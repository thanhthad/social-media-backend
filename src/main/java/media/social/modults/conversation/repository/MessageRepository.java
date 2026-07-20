package media.social.modults.conversation.repository;
import media.social.modults.conversation.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversation_Id(
            Long conversationId,
            Pageable pageable
    );

    Optional<Message> findFirstByConversation_IdOrderByCreatedAtDesc(Long conversationId);
}