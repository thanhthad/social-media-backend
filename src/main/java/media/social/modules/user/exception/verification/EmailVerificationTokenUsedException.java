package media.social.modules.user.exception.verification;

public class EmailVerificationTokenUsedException extends RuntimeException {
    public EmailVerificationTokenUsedException(String message) {
        super(message);
    }
}
