package media.social.modults.user.dto.response.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FollowUserResponse {

    private Long userId;

    private String username;

    private String avatarUrl;

}