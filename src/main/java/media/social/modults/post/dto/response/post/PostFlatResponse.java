package media.social.modults.post.dto.response.post;

import lombok.Getter;
import lombok.Setter;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.post.enums.Visibility;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostFlatResponse {

    public PostFlatResponse(
            Long id,
            String content,
            Visibility visibility,
            LocalDateTime createdAt,
            Long userId,
            String username,
            String avatarUrl,
            Long commentCount,
            Long reactionCount
    ) {
        this.id = id;
        this.content = content;
        this.visibility = visibility;
        this.createdAt = createdAt;
        this.userId = userId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.commentCount = commentCount;
        this.reactionCount = reactionCount;
    }

    private Long id;
    private String content;
    private Visibility visibility;
    private LocalDateTime createdAt;

    private Long userId;
    private String username;
    private String avatarUrl;

    private Long commentCount;
    private Long reactionCount;

    private Boolean reacted;
    private ReactionType myReactionType;
}