package media.social.modults.conversation.exception;

public class UserInMemberAlreadyExists extends RuntimeException {
    public UserInMemberAlreadyExists(String message) {
        super(message);
    }
}
