package media.social.modules.post.dto.response.reaction;

import lombok.*;
import media.social.modules.post.enums.ReactionType;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserReactionResponse {
    private Long id;
    private String username;
    private String fullName;
    private String avatarUrl;
    private ReactionType type;
    private LocalDateTime createdAt;
}