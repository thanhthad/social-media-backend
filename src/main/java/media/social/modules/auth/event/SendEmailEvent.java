package media.social.modules.auth.event;

import java.time.Instant;

public record SendEmailEvent(
        String to,
        String token,
        EmailType type,
        Instant timestamp
) {
    public SendEmailEvent(String to, String token, EmailType type) {
        this(to, token, type, Instant.now());
    }
}
