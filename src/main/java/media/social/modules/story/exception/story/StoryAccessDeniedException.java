package media.social.modules.story.exception.story;

public class StoryAccessDeniedException extends RuntimeException {
    public StoryAccessDeniedException(String message) {
        super(message);
    }
}
