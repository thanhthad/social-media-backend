package media.social.modults.user.dto.response.common;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private LocalDateTime createdAt;
}