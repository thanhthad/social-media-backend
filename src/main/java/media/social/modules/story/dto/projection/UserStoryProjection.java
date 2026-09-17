package media.social.modules.story.dto.projection;

import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;

public interface UserStoryProjection {

    Long getStoryId();

    Long getUserId();

    String getUsername();

    String getAvatarUrl();

    String getContent();

    Visibility getVisibility();

    MediaType getMediaType();

    String getUrl();

    LocalDateTime getExpiresAt();

    LocalDateTime getCreatedAt();

    LocalDateTime getUpdatedAt();
}