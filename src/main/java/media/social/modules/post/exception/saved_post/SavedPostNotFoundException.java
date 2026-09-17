package media.social.modules.post.exception.saved_post;

public class SavedPostNotFoundException extends RuntimeException {
    public SavedPostNotFoundException(String message) {
        super(message);
    }
}
