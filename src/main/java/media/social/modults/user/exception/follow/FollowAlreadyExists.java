package media.social.modults.user.exception.follow;

public class FollowAlreadyExists extends RuntimeException {
    public FollowAlreadyExists(String message) {
        super(message);
    }
}
