package media.social.modults.conversation.repository;

import media.social.modults.conversation.entity.MessageMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageMediaRepository
        extends JpaRepository<MessageMedia,Long> {


    List<MessageMedia> findByMessageId(
            Long messageId
    );


    void deleteByMessageIdAndIdIn(
            Long messageId,
            List<Long> ids
    );

}