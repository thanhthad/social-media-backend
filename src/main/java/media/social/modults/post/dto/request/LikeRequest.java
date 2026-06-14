package media.social.modults.post.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LikeRequest {

    @NotNull(message = "PostId is required")
    private Long postId;
}