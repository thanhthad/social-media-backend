package media.social.modules.user.dto.response.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowStatCacheResponse {

    private Long totalFollower;

    private Long totalFollowing;
}
