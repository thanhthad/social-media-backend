package media.social.modules.auth.exception.password;

public class AccountAlreadyLockedException extends RuntimeException {
    public AccountAlreadyLockedException(String message) {
        super(message);
    }
}
