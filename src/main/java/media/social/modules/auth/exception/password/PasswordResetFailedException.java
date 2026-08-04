package media.social.modules.auth.exception.password;

public class PasswordResetFailedException extends RuntimeException {
    public PasswordResetFailedException(String message) {
        super(message);
    }
}
