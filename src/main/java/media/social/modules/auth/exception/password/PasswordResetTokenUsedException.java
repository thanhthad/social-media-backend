package media.social.modules.auth.exception.password;

public class PasswordResetTokenUsedException extends RuntimeException {
    public PasswordResetTokenUsedException(String message) {
        super(message);
    }
}
