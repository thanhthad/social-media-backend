package media.social.modules.user.dto.projection;

import media.social.modules.auth.Enum.Status;

import java.time.LocalDateTime;

public interface AdminUserProjection {

    Long getId();

    String getUsername();

    Status getStatus();

    String getAvatarUrl();

    LocalDateTime getCreatedAt();

    LocalDateTime getLastLoginAt();

    LocalDateTime getLastActiveAt();
}