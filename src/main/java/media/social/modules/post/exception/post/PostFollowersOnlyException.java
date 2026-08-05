package media.social.modules.post.exception.post;

public class PostFollowersOnlyException extends RuntimeException {
    public PostFollowersOnlyException(String message) {
        super(message);
    }
}
