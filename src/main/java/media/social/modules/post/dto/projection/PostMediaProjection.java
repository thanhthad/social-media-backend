package media.social.modules.post.dto.projection;

import media.social.modules.post.enums.MediaType;

public interface PostMediaProjection {

    Long getPostMediaId();

    String getUrl();

    MediaType getType();
}