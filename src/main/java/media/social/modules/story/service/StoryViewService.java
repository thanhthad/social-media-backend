package media.social.modules.story.service;

import media.social.modules.story.dto.response.StoryViewResponse;
import java.util.List;

public interface StoryViewService {

    void view(
            Long storyId
    );

    List<StoryViewResponse> getViewers(
            Long storyId
    );
}