package media.social.modules.post.dto.projection;

import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;

public interface PostFlatProjection {

    Long getId();

    String getContent();

    Visibility getVisibility();

    LocalDateTime getCreatedAt();


    Long getUserId();

    String getUsername();

    String getAvatarUrl();


    Long getCommentCount();

    Long getReactionCount();
}