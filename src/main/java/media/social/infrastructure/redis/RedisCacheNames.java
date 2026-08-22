package media.social.infrastructure.redis;

public final class RedisCacheNames {

    private RedisCacheNames() {
        // Prevent instantiation
    }

    public static final String USERS = "users";
    public static final String USER_PROFILE = "userProfile";
    public static final String FRIENDSHIP_COUNT = "friendShipCount";
    public static final String POSTS = "posts";
    public static final String USER_REACTIONS = "user_reactions";
    public static final String USER_STORIES = "userStories";
    public static final String DATING_PROFILE = "datingProfile";
    public static final String MUTUAL_FRIEND_COUNT= "mutualFriendCount";
    public static final String MUTUAL_FRIEND_AVATARS = "mutualFriendAvatars";
}
