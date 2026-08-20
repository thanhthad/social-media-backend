package media.social.modules.user.service;

import media.social.modules.user.dto.response.friend.FriendSuggestionResponse;
import media.social.modules.user.dto.response.friend.MutualFriendResponse;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FriendshipService {

    void sendFriendRequest(Long targetUserId);

    void acceptFriendRequest(Long requesterId);

    void rejectFriendRequest(Long requesterId);

    Page<FriendshipUserResponse> getFriends(
            Long userId,
            Pageable pageable
    );

    Page<FriendshipUserResponse> getMyFriends(
            Pageable pageable
    );

    List<FriendSuggestionResponse> getSuggestedUsers();

    List<MutualFriendResponse> getMutualFriends(Long targetUserId);
}

