package media.social.modults.others.dto.response;

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