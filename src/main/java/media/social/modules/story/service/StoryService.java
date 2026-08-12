package media.social.modules.story.service;

import media.social.modules.story.dto.request.CreateStoryRequest;
import media.social.modules.story.dto.request.UpdateStoryVisibilityRequest;
import media.social.modules.story.dto.response.MyStoryResponse;
import media.social.modules.story.dto.response.StoryFeedResponse;
import media.social.modules.story.dto.response.UserStoryResponse;

import java.util.List;

public interface StoryService {

    void create(
            CreateStoryRequest request
    );

    List<StoryFeedResponse> getFeed(
    );

    List<UserStoryResponse> getUserStoriesBeforeExpire(
            Long targetUserId
    );


    List<UserStoryResponse> getUserStories(
            Long targetUserId
    );

    List<MyStoryResponse> getMyStories(
    );

    void updateVisibility(
            Long storyId,
            UpdateStoryVisibilityRequest request
    );

    void delete(
            Long storyId
    );
}