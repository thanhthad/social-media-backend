package media.social.modules.auth.exception.verification;

public class EmailVerificationTokenInvalidException extends RuntimeException {
    public EmailVerificationTokenInvalidException(String message) {
        super(message);
    }
}
