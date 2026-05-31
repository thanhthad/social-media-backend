package media.social.modults.post.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeRequest {

    @NotNull(message = "UserId is required")
    private Long userId;

    @NotNull(message = "PostId is required")
    private Long postId;
}