package media.social.modules.story.dto.response;

import lombok.*;
import media.social.modules.post.enums.ReactionType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryViewResponse {
    private Long userId;
    private String username;
    private String avatarUrl;
    private LocalDateTime viewedAt;
    private ReactionType reactionType;
    private LocalDateTime reactionAt;
}
