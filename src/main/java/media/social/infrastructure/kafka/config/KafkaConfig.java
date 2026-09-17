package media.social.infrastructure.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${app.kafka.topics.notification:social.notification.events}")
    private String notificationTopicName;

    @Value("${app.kafka.topics.email:social.email.events}")
    private String emailTopicName;

    @Value("${app.kafka.topics.user-profile:user-profile-events}")
    private String userProfileTopicName;

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(notificationTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic emailTopic() {
        return TopicBuilder.name(emailTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic userProfileTopic() {
        return TopicBuilder.name(userProfileTopicName)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public CommonErrorHandler errorHandler() {
        // Retry 3 times with 1000ms delay between attempts
        FixedBackOff fixedBackOff = new FixedBackOff(1000L, 3L);
        return new DefaultErrorHandler((record, exception) -> {
            log.error("Kafka consumer exhausted retries for record with key '{}' on topic '{}': {}",
                    record.key(), record.topic(), exception.getMessage());
        }, fixedBackOff);
    }
}
