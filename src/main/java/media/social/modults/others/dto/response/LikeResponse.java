package media.social.modults.others.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class LikeResponse {

    private Long id;

    private Long userId;
    private String username;

    private Long postId;

    private LocalDateTime createdAt;
}