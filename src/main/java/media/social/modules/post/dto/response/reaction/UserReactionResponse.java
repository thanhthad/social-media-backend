package media.social.modules.post.dto.response.reaction;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserReactionResponse {
    private Long id;
    private String email;
    private String avatarUrl;
    private LocalDateTime createdAt;
}