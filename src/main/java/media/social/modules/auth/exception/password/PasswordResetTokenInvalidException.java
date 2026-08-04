package media.social.modules.auth.exception.password;

public class PasswordResetTokenInvalidException extends RuntimeException {
    public PasswordResetTokenInvalidException(String message) {
        super(message);
    }
}
