package media.social.modules.user.exception.block;

public class BlockAlreadyExistsException extends RuntimeException {
    public BlockAlreadyExistsException(String message) {
        super(message);
    }
}
