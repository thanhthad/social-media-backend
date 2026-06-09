package media.social.modults.user.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.user.dto.response.FollowUserResponse;
import media.social.modults.user.entity.Follow;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.follow.FollowAlreadyExists;
import media.social.modults.user.exception.follow.FollowNotFoundException;
import media.social.modults.user.repository.FollowRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.FollowService;
import media.social.modults.user.service.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserServiceDomain userServiceDomain;

    @Override
    @Transactional
    public void followUser(Long targetUserId) {

        userServiceDomain.validateUserExists(targetUserId);

        Long currentUserId = UserContextHolder.getUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        if (followRepository.existsByFollower_IdAndFollowing_Id(currentUserId, targetUserId)) {
            throw new FollowAlreadyExists("You already follow this user with id:"+targetUserId);
        }

        User follower = userRepository.findById(currentUserId)
                .orElseThrow(() -> new FollowNotFoundException("User not found"));

        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new FollowNotFoundException("Target user not found"));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);

        followRepository.save(follow);
    }

    @Override
    @Transactional
    public void unfollowUser(Long targetUserId) {
        userServiceDomain.validateUserExists(targetUserId);

        Long currentUserId = UserContextHolder.getUserId();

        followRepository.deleteByFollower_IdAndFollowing_Id(
                currentUserId,
                targetUserId
        );
    }

    @Override
    public boolean isFollowing(Long targetUserId) {
        userServiceDomain.validateUserExists(targetUserId);

        Long currentUserId = UserContextHolder.getUserId();

        return followRepository.existsByFollower_IdAndFollowing_Id(
                currentUserId,
                targetUserId
        );
    }

    @Override
    public Page<FollowUserResponse> getFollowers(Long userId, Pageable pageable) {
        userServiceDomain.validateUserExists(userId);
        return followRepository.getFollowers(userId, pageable);
    }

    @Override
    public Page<FollowUserResponse> getFollowing(Long userId, Pageable pageable) {
        userServiceDomain.validateUserExists(userId);
        return followRepository.getFollowing(userId, pageable);
    }

    @Override
    public Page<FollowUserResponse> getMyFollowers(Pageable pageable) {
        Long currentUserId = UserContextHolder.getUserId();
        return followRepository.getFollowers(currentUserId, pageable);
    }

    @Override
    public Page<FollowUserResponse> getMyFollowing(Pageable pageable) {
        Long currentUserId = UserContextHolder.getUserId();
        return followRepository.getFollowing(currentUserId, pageable);
    }
}

