package media.social.modules.post.dto.response.follow;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FollowResponse {

    private Long followerId;
    private Long followingId;

    private LocalDateTime createdAt;
}