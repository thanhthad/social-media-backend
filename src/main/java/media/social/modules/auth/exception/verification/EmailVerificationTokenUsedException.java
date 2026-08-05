package media.social.modules.auth.exception.verification;

public class EmailVerificationTokenUsedException extends RuntimeException {
    public EmailVerificationTokenUsedException(String message) {
        super(message);
    }
}
