package media.social.modults.post.exception.like;

public class LikeAlreadyExistsException extends RuntimeException {
    public LikeAlreadyExistsException(String message) {
        super(message);
    }
}
