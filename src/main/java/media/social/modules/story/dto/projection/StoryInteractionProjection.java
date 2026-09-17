package media.social.modules.story.dto.projection;

import media.social.modules.post.enums.ReactionType;

import java.time.LocalDateTime;

public interface StoryInteractionProjection {

    Long getStoryId();

    Long getUserId();

    String getUsername();

    String getAvatarUrl();

    LocalDateTime getViewAt();

    ReactionType getReactionType();

    LocalDateTime getReactionAt();
}