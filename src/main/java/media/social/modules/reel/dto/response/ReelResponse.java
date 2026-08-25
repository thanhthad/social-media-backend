package media.social.modules.reel.dto.response;

import lombok.*;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.Visibility;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelResponse {

    // ---- Post fields ----
    private Long id;

    private String content;

    private Visibility visibility;

    private LocalDateTime createdAt;

    // ---- Author info ----
    private Long userId;

    private String username;

    private String avatarUrl;

    // ---- Interaction counts ----
    private Long commentCount;

    private Long reactionCount;

    private boolean reacted;

    private ReactionType myReactionType;

    // ---- Reel-specific fields ----
    private String videoUrl;

    private String thumbnailUrl;

    private Integer durationSeconds;

    private Integer width;

    private Integer height;

    private Long viewCount;

    private Long shareCount;
}
