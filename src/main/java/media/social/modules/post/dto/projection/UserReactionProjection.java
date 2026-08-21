package media.social.modules.post.dto.projection;

import java.time.LocalDateTime;

public interface UserReactionProjection {

    Long getId();

    String getEmail();

    String getAvatarUrl();

    LocalDateTime getCreatedAt();
}
