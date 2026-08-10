package media.social.modules.post.dto.response.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class PostFlatResponse {

    private Long id;
    private String content;
    private Visibility visibility;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String avatarUrl;

    private Long commentCount;
    private Long reactionCount;

}