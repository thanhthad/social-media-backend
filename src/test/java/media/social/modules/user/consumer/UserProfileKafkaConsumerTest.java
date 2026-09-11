package media.social.modules.user.consumer;

import media.social.modules.user.event.ProfileUpdatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class UserProfileKafkaConsumerTest {

    @InjectMocks
    private UserProfileKafkaConsumer userProfileKafkaConsumer;

    @Test
    void consumeProfileUpdated_DoesNotThrow() {
        ProfileUpdatedEvent event = new ProfileUpdatedEvent(
                1L,
                "John Doe",
                "Hello world",
                "2000-01-01",
                "MALE",
                Instant.now()
        );

        assertDoesNotThrow(() -> userProfileKafkaConsumer.consumeProfileUpdated(event));
    }
}
