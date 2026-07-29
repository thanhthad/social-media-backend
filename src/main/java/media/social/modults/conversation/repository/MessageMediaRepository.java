package media.social.modults.conversation.repository;

import media.social.modults.conversation.entity.MessageMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MessageMediaRepository
        extends JpaRepository<MessageMedia,Long> {

    @Query("""
    SELECT mm
    FROM MessageMedia mm
    WHERE mm.message.id IN :messageIds
    """)
    List<MessageMedia> findByMessageIds(
            List<Long> messageIds
    );

}