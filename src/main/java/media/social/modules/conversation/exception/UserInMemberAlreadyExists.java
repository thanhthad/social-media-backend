package media.social.modules.conversation.exception;

public class UserInMemberAlreadyExists extends RuntimeException {
    public UserInMemberAlreadyExists(String message) {
        super(message);
    }
}
