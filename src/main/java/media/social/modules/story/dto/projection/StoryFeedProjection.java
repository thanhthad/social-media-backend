package media.social.modules.story.dto.projection;

import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;

public interface StoryFeedProjection {

    Long getUserId();

    String getAvatarUrl();

    String getContent();

    Visibility getVisibility();

    MediaType getMediaType();

    String getUrl();
}