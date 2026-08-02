package media.social.modules.user.service;

import media.social.modules.user.dto.response.user.FollowCountResponse;
import media.social.modules.user.dto.response.user.FollowUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowService {

    void followUser(Long targetUserId);

    void unfollowUser(Long targetUserId);

    boolean isFollowing(Long targetUserId);

    FollowCountResponse getProfile(Long userId);

    FollowCountResponse geMytProfile();


    //-> see user follower (not inlucde me)
    Page<FollowUserResponse> getFollowers(Long userId, Pageable pageable);

    Page<FollowUserResponse> getFollowing(Long userId, Pageable pageable);

    //userId from jwtToken -> see my follower
    Page<FollowUserResponse> getMyFollowers(Pageable pageable);

    Page<FollowUserResponse> getMyFollowing(Pageable pageable);
}
