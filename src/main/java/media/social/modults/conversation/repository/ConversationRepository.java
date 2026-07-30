package media.social.modults.conversation.repository;

import media.social.modults.conversation.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

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
}