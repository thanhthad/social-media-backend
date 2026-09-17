package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.swipe.CreateDatingSwipeRequest;
import media.social.modules.dating.dto.response.swipe.DatingSwipeResponse;

import java.util.List;

public interface DatingSwipeService {

    DatingSwipeResponse swipe(CreateDatingSwipeRequest request);

    List<DatingSwipeResponse> getMySwipes();

    List<DatingSwipeResponse> getMyLikes();

    void deleteSwipe(Long targetUserId);
}