package media.social.modules.user.service;

import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FriendshipService {

    boolean areFriends(Long userId, Long targetUserId);

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
}

