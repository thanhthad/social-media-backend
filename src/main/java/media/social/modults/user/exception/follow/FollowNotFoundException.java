package media.social.modults.user.exception.follow;

public class FollowNotFoundException extends RuntimeException {
    public FollowNotFoundException(String message) {
        super(message);
    }
}
