package media.social.modules.user.exception.role;

public class UserRoleAlreadyExistsException extends RuntimeException {
    public UserRoleAlreadyExistsException(String message) {
        super(message);
    }
}
