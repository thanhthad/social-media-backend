package media.social.modules.user.event;

import java.time.Instant;

public record ProfileUpdatedEvent(
        Long userId,
        String fullName,
        String bio,
        String dateOfBirth,
        String gender,
        Instant occurredAt
) {
}