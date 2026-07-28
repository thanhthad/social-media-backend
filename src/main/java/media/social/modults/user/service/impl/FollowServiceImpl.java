package media.social.modults.user.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.notification.service.NotificationService;
import media.social.modults.user.Enum.RoleName;
import media.social.modults.user.dto.response.user.FollowCountResponse;
import media.social.modults.user.dto.response.user.FollowUserResponse;
import media.social.modults.user.entity.Follow;
import media.social.modults.user.entity.FollowId;
import media.social.modults.user.entity.User;
import media.social.modults.user.exception.follow.FollowAlreadyExistsException;
import media.social.modults.user.exception.follow.FollowNotFoundException;
import media.social.modults.user.repository.FollowRepository;
import media.social.modults.user.repository.UserRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.FollowService;
import media.social.modults.user.service.cache.UserCacheService;
import media.social.modults.user.service.domain.UserRoleServiceDomain;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UserServiceDomain userServiceDomain;
    private final NotificationService notificationService;
    private final UserRoleServiceDomain userRoleServiceDomain;
    private final UserCacheService followCacheService;

    @Override
    @Transactional
    public void followUser(Long targetUserId) {

        userServiceDomain.validateUserExists(targetUserId);

        Long currentUserId = UserContextHolder.getUserId();

        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself");
        }

        if(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN) || userRoleServiceDomain.hasRole(targetUserId,RoleName.MODERATOR)){
            throw new AccessDeniedException("You do not have permit to follow this user");
        }


        if (followRepository.existsByFollower_IdAndFollowing_Id(currentUserId, targetUserId)) {
            throw new FollowAlreadyExistsException("You already follow this user with id:"+targetUserId);
        }

        User follower = userRepository.findById(currentUserId)
                .orElseThrow(() -> new FollowNotFoundException("User not found"));

        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new FollowNotFoundException("Target user not found"));

        Follow follow = Follow.builder()
                .id(new FollowId(
                        follower.getId(),
                        following.getId()
                ))
                .follower(follower)
                .following(following)
                .build();

        followRepository.save(follow);

        followCacheService.evict(currentUserId);
        followCacheService.evict(targetUserId);

        notificationService.create(
                following,
                follower,
                EntityType.USER,
                follower.getId(),
                NotificationType.FOLLOW
        );
    }

    @Override
    @Transactional
    public void unfollowUser(Long targetUserId) {
        userServiceDomain.validateUserExists(targetUserId);

        Long currentUserId = UserContextHolder.getUserId();

        long deleted = followRepository.deleteByFollower_IdAndFollowing_Id(
                currentUserId,
                targetUserId
        );

        if (deleted == 0) {
            throw new FollowNotFoundException("You are not following this user");
        }

        followCacheService.evict(currentUserId);
        followCacheService.evict(targetUserId);

        notificationService.delete(
                targetUserId,
                currentUserId,
                EntityType.USER,
                currentUserId,
                NotificationType.FOLLOW
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long targetUserId) {
        userServiceDomain.validateUserExists(targetUserId);

        Long currentUserId = UserContextHolder.getUserId();

        return followRepository.existsByFollower_IdAndFollowing_Id(
                currentUserId,
                targetUserId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountResponse getProfile(Long userId) {
        userServiceDomain.validateUserExists(userId);

        Long total_following = followRepository.countByFollowing_Id(userId);

        Long total_follower = followRepository.countByFollower_Id(userId);

        return FollowCountResponse.builder()
                .total_following(total_following)
                .total_follower(total_follower)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowCountResponse geMytProfile() {
        Long userId = UserContextHolder.getUserId();

        Long total_following = followRepository.countByFollowing_Id(userId);

        Long total_follower = followRepository.countByFollower_Id(userId);
        return FollowCountResponse.builder()
                .total_following(total_following)
                .total_follower(total_follower)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getFollowers(Long userId, Pageable pageable) {
        userServiceDomain.validateUserExists(userId);
        return followRepository.getFollowers(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getFollowing(Long userId, Pageable pageable) {
        userServiceDomain.validateUserExists(userId);
        return followRepository.getFollowing(userId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getMyFollowers(Pageable pageable) {
        Long currentUserId = UserContextHolder.getUserId();
        return followRepository.getFollowers(currentUserId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUserResponse> getMyFollowing(Pageable pageable) {
        Long currentUserId = UserContextHolder.getUserId();
        return followRepository.getFollowing(currentUserId, pageable);
    }
}

