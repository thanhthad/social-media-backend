package media.social.modults.post.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LikeResponse {

    private Long userId;

    private String username;

    private Long postId;

    private LocalDateTime createdAt;
}