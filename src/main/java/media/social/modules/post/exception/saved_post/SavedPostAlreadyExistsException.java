package media.social.modules.post.exception.saved_post;

public class SavedPostAlreadyExistsException extends RuntimeException {
    public SavedPostAlreadyExistsException(String message) {
        super(message);
    }
}
