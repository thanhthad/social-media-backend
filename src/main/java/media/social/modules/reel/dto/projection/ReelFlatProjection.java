package media.social.modules.reel.dto.projection;

import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;

public interface ReelFlatProjection {

    Long getId();

    String getContent();

    Visibility getVisibility();

    LocalDateTime getCreatedAt();

    Long getUserId();

    String getUsername();

    String getAvatarUrl();

    Long getCommentCount();

    Long getReactionCount();

    // ReelDetail fields
    Integer getDurationSeconds();

    Integer getWidth();

    Integer getHeight();

    String getThumbnailUrl();

    Long getViewCount();

    Long getShareCount();
}
