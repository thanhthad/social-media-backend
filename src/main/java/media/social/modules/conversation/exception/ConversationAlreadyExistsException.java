package media.social.modules.conversation.exception;

public class ConversationAlreadyExistsException extends RuntimeException {
    public ConversationAlreadyExistsException(String message) {
        super(message);
    }
}
