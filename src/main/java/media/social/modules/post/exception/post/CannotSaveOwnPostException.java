package media.social.modules.post.exception.post;

public class CannotSaveOwnPostException extends RuntimeException {
    public CannotSaveOwnPostException(String message) {
        super(message);
    }
}
