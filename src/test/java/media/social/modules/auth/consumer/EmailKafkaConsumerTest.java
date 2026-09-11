package media.social.modules.auth.consumer;

import media.social.modules.auth.event.EmailType;
import media.social.modules.auth.event.SendEmailEvent;
import media.social.modules.auth.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailKafkaConsumerTest {

    @InjectMocks
    private EmailKafkaConsumer emailKafkaConsumer;

    @Mock
    private EmailService emailService;

    @Test
    void consumeEmailEvent_VerifyEmail_CallsSendVerificationEmail() {
        SendEmailEvent event = new SendEmailEvent(
                "user@example.com",
                "token-12345",
                EmailType.VERIFY_EMAIL,
                Instant.now()
        );

        emailKafkaConsumer.consumeEmailEvent(event);

        verify(emailService).sendVerificationEmail("user@example.com", "token-12345");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    void consumeEmailEvent_ResetPassword_CallsSendPasswordResetEmail() {
        SendEmailEvent event = new SendEmailEvent(
                "user@example.com",
                "reset-token-67890",
                EmailType.RESET_PASSWORD,
                Instant.now()
        );

        emailKafkaConsumer.consumeEmailEvent(event);

        verify(emailService).sendPasswordResetEmail("user@example.com", "reset-token-67890");
        verifyNoMoreInteractions(emailService);
    }

    @Test
    void consumeEmailEvent_WhenEmailServiceFails_ThrowsExceptionForRetry() {
        SendEmailEvent event = new SendEmailEvent(
                "user@example.com",
                "token-12345",
                EmailType.VERIFY_EMAIL,
                Instant.now()
        );

        doThrow(new RuntimeException("SMTP connection refused"))
                .when(emailService).sendVerificationEmail("user@example.com", "token-12345");

        assertThrows(RuntimeException.class, () ->
                emailKafkaConsumer.consumeEmailEvent(event)
        );

        verify(emailService).sendVerificationEmail("user@example.com", "token-12345");
    }
}
