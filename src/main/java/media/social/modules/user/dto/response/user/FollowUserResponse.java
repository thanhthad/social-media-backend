package media.social.modules.user.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowUserResponse {

    private Long userId;

    private String username;

    private String avatarUrl;

}