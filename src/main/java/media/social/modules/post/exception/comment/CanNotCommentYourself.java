package media.social.modules.post.exception.comment;

public class CanNotCommentYourself extends RuntimeException {
    public CanNotCommentYourself(String message) {
        super(message);
    }
}
