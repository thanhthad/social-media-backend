package media.social.modults.user.service;

import media.social.modults.user.dto.response.FollowUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FollowService {

    void followUser(Long targetUserId);

    void unfollowUser(Long targetUserId);

    boolean isFollowing(Long targetUserId);

    //-> see user follower (not inlucde me)
    Page<FollowUserResponse> getFollowers(Long userId, Pageable pageable);

    Page<FollowUserResponse> getFollowing(Long userId, Pageable pageable);

    //userId from jwtToken -> see my follower
    Page<FollowUserResponse> getMyFollowers(Pageable pageable);

    Page<FollowUserResponse> getMyFollowing(Pageable pageable);
}
