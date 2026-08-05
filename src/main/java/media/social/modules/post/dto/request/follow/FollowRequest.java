package media.social.modules.post.dto.request.follow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FollowRequest {

    @NotNull(message = "FollowerId is required")
    @Positive(message = "FollowerId must be positive")
    private Long followerId;

    @NotNull(message = "FollowingId is required")
    @Positive(message = "FollowingId must be positive")
    private Long followingId;
}