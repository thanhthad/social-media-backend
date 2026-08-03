package media.social.modules.user.exception.verification;

public class EmailSendFailedException extends RuntimeException {
    public EmailSendFailedException(String message) {
        super(message);
    }
}
