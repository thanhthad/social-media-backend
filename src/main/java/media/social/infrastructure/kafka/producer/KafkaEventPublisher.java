package media.social.infrastructure.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(
            String topic,
            String key,
            Object event
    ) {
        kafkaTemplate.send(topic, key, event);
    }
}