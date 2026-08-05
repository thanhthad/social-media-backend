package media.social.modules.user.dto.response.user;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.auth.Enum.Status;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminUserResponse {

    private Long id;

    private String username;

    private Status status;

    private String avatarUrl;

    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private LocalDateTime lastActiveAt;
}
