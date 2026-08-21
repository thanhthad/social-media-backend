package media.social.modules.post.dto.projection;

import media.social.modules.post.enums.MediaType;

public interface ListPostMediaProjection {

    Long getPostMediaId();

    Long getPostId();

    String getUrl();

    MediaType getType();
}
