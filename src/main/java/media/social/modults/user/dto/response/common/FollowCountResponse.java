package media.social.modults.user.dto.response.common;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FollowCountResponse {

    private Long total_follower;

    private Long total_following;
}
