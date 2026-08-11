package media.social.modules.post.exception.saved_post;

public class PostFriendsOnlyException extends RuntimeException {
    public PostFriendsOnlyException(String message) {
        super(message);
    }
}
