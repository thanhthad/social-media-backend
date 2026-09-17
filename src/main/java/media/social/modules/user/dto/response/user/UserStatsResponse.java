package media.social.modules.user.dto.response.user;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsResponse {
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private long totalPost;
    private long totalReel;
    private long totalFriend;
    private long totalLikesReceived;
    private String friendshipStatus; // "SELF", "FRIENDS", "PENDING_SENT", "PENDING_RECEIVED", "NONE", "BLOCKED"
    private boolean isOnline;
}
