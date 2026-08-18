package media.social.modules.dating.exception.photo;

public class DatingProfilePhotoLimitExceededException
        extends RuntimeException {

    public DatingProfilePhotoLimitExceededException(String message) {
        super(message);
    }
}