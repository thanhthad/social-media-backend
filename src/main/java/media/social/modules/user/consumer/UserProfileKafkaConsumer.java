package media.social.modules.user.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.user.event.ProfileUpdatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserProfileKafkaConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.user-profile:user-profile-events}",
            groupId = "${spring.kafka.consumer.group-id:social-group}"
    )
    public void consumeProfileUpdated(ProfileUpdatedEvent event) {
        log.info("Received ProfileUpdatedEvent for userId: {}, fullName: '{}', occurredAt: {}",
                event.userId(), event.fullName(), event.occurredAt());
        // Can be extended for audit trail, search indexing, or downstream service sync
    }
}
