package media.social.modules.notification.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.notification.event.NotificationEvent;
import media.social.modules.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.notification:social.notification.events}",
            groupId = "${spring.kafka.consumer.group-id:social-group}"
    )
    public void consumeNotification(NotificationEvent event) {
        log.info("Received NotificationEvent from Kafka for receiverId: {}, type: {}, entityType: {}, entityId: {}",
                event.receiverId(), event.type(), event.entityType(), event.entityId());
        try {
            notificationService.processNotificationEvent(event);
            log.debug("Successfully processed NotificationEvent for receiverId: {}", event.receiverId());
        } catch (Exception e) {
            log.error("Failed to process NotificationEvent for receiverId {}: {}",
                    event.receiverId(), e.getMessage(), e);
            throw e;
        }
    }
}
