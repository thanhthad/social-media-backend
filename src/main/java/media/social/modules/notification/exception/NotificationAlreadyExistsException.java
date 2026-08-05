package media.social.modules.notification.exception;

public class NotificationAlreadyExistsException extends RuntimeException {
    public NotificationAlreadyExistsException(String message) {
        super(message);
    }
}
