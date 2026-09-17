package media.social.modules.auth.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.auth.event.SendEmailEvent;
import media.social.modules.auth.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailKafkaConsumer {

    private final EmailService emailService;

    @KafkaListener(
            topics = "${app.kafka.topics.email:social.email.events}",
            groupId = "${spring.kafka.consumer.group-id:social-group}"
    )
    public void consumeEmailEvent(SendEmailEvent event) {
        log.info("Received SendEmailEvent for recipient: '{}', type: '{}'", event.to(), event.type());
        try {
            switch (event.type()) {
                case VERIFY_EMAIL -> emailService.sendVerificationEmail(event.to(), event.token());
                case RESET_PASSWORD -> emailService.sendPasswordResetEmail(event.to(), event.token());
            }
            log.info("Successfully processed {} email for '{}'", event.type(), event.to());
        } catch (Exception e) {
            log.error("Failed to process SendEmailEvent for '{}': {}", event.to(), e.getMessage(), e);
            throw e;
        }
    }
}
