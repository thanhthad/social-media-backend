package media.social.modules.post.dto.projection;

import media.social.modules.post.enums.ReactionType;

import java.time.LocalDateTime;

public interface CommentProjection {

    Long getCommentId();

    Long getUserId();

    String getUsername();

    String getAvatarUrl();

    Long getParentId();

    String getContent();

    LocalDateTime getCreatedAt();

    Long getTotalReplies();

    Long getTotalReactions();

    ReactionType getMyReaction();
}