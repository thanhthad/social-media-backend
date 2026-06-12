package media.social.modults.post.exception.comment;

public class CommentAlreadyExistsException extends RuntimeException {
    public CommentAlreadyExistsException(String message) {
        super(message);
    }
}
