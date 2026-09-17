package media.social.modules.story.service.cache;

import media.social.modules.story.dto.response.UserStoryResponse;

import java.util.List;

public interface StoryCacheService {

    List<UserStoryResponse> getUserStories(Long targetUserId);

    void evictUserStories(Long userId);
}