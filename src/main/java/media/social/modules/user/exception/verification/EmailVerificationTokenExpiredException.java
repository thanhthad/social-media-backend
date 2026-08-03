package media.social.modules.user.exception.verification;

public class EmailVerificationTokenExpiredException extends RuntimeException {
    public EmailVerificationTokenExpiredException(String message) {
        super(message);
    }
}
