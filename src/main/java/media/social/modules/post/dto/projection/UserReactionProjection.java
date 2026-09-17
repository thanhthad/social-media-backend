package media.social.modules.post.dto.projection;

import media.social.modules.post.enums.ReactionType;
import java.time.LocalDateTime;

public interface UserReactionProjection {

    Long getId();

    String getUserName();

    String getFullName();

    String getAvatarUrl();

    ReactionType getType();

    LocalDateTime getCreatedAt();
}
