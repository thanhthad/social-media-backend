package media.social.modules.post.dto.projection;

import java.time.LocalDateTime;

public interface UserReactionProjection {

    Long getId();

    String getUserName();

    String getAvatarUrl();

    LocalDateTime getCreatedAt();
}
