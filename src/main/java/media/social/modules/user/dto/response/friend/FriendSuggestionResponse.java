package media.social.modules.user.dto.response.friend;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FriendSuggestionResponse {

    private Long userId;
    private String username;
    private String avatarUrl;

    private Long mutualCount;
}