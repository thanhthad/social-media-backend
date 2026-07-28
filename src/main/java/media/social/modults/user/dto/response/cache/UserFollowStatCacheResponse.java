package media.social.modults.user.dto.response.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserFollowStatCacheResponse {

    private Long totalFollower;

    private Long totalFollowing;
}
