package media.social.modules.user.exception.verification;

public class EmailVerificationTokenInvalidException extends RuntimeException {
    public EmailVerificationTokenInvalidException(String message) {
        super(message);
    }
}
