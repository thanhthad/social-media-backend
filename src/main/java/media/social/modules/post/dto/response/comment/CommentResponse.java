package media.social.modules.post.dto.response.comment;

import lombok.*;
import media.social.modules.post.enums.ReactionType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long commentId;

    private Long userId;

    private String username;

    private String avatarUrl;

    private Long parentId;

    private String content;

    private LocalDateTime createdAt;

    private Long totalReplies;

    private Long totalReactions;

    private ReactionType myReaction;

}