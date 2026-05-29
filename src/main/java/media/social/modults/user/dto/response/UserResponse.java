package media.social.modults.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modults.user.Enum.Role;
import media.social.modults.user.Enum.Status;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserResponse {

    private Long id;

    private String username;

    private String email;

    private Role role;

    private Status status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    private LocalDateTime lastActiveAt;
}