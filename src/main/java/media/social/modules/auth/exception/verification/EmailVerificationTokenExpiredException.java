package media.social.modules.auth.exception.verification;

public class EmailVerificationTokenExpiredException extends RuntimeException {
    public EmailVerificationTokenExpiredException(String message) {
        super(message);
    }
}
