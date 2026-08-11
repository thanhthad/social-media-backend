package media.social.modules.user.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.repository.FriendshipRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.FriendshipService;
import media.social.modules.user.service.cache.UserCacheService;
import media.social.modules.user.service.domain.UserRoleServiceDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserServiceDomain userServiceDomain;
    private final UserRoleServiceDomain userRoleServiceDomain;
    private final NotificationService notificationService;
    private final UserCacheService userCacheService;

    @Override
    @Transactional(readOnly = true)
    public boolean areFriends(Long userId, Long targetUserId) {
        return friendshipRepository.areFriends(
                userId,
                targetUserId,
                FriendshipStatus.ACCEPTED
        );
    }

    @Override
    @Transactional
    public void sendFriendRequest(Long targetUserId) {
        Long currentUserId = UserContextHolder.getUserId();

        validateFriendshipTarget(currentUserId, targetUserId);

        Long userOneId = Math.min(currentUserId, targetUserId);
        Long userTwoId = Math.max(currentUserId, targetUserId);

        if (friendshipRepository
                .findByUserOne_IdAndUserTwo_Id(userOneId, userTwoId)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "A friendship or friend request already exists"
            );
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        User userOne = currentUserId.equals(userOneId)
                ? currentUser
                : targetUser;

        User userTwo = currentUserId.equals(userTwoId)
                ? currentUser
                : targetUser;

        Friendship friendship = Friendship.builder()
                .userOne(userOne)
                .userTwo(userTwo)
                .requester(currentUser)
                .status(FriendshipStatus.PENDING)
                .build();

        friendshipRepository.save(friendship);

        notificationService.create(
                targetUser,
                currentUser,
                EntityType.USER,
                currentUser.getId(),
                NotificationType.FRIEND_REQUEST
        );
    }

    @Override
    @Transactional
    public void acceptFriendRequest(Long requesterId) {
        Long currentUserId = UserContextHolder.getUserId();

        validateFriendshipTarget(currentUserId, requesterId);

        Long userOneId = Math.min(currentUserId, requesterId);
        Long userTwoId = Math.max(currentUserId, requesterId);

        Friendship friendship = friendshipRepository
                .findByUserOne_IdAndUserTwo_Id(userOneId, userTwoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Friend request not found")
                );

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This friend request cannot be accepted"
            );
        }

        if (!friendship.getRequester().getId().equals(requesterId)) {
            throw new AccessDeniedException(
                    "You can only accept a friend request sent to you"
            );
        }

        if (!friendship.getUserOne().getId().equals(currentUserId)
                && !friendship.getUserTwo().getId().equals(currentUserId)) {

            throw new AccessDeniedException(
                    "You are not part of this friendship"
            );
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);

        evictFriendshipCache(currentUserId, requesterId);
    }

    @Override
    @Transactional
    public void rejectFriendRequest(Long requesterId) {
        Long currentUserId = UserContextHolder.getUserId();

        validateFriendshipTarget(currentUserId, requesterId);

        Long userOneId = Math.min(currentUserId, requesterId);
        Long userTwoId = Math.max(currentUserId, requesterId);

        Friendship friendship = friendshipRepository
                .findByUserOne_IdAndUserTwo_Id(userOneId, userTwoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Friend request not found")
                );

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new IllegalArgumentException(
                    "This is not a pending friend request"
            );
        }

        if (!friendship.getRequester().getId().equals(requesterId)) {
            throw new AccessDeniedException(
                    "You can only reject a friend request sent by this user"
            );
        }

        friendshipRepository.delete(friendship);

        notificationService.delete(
                currentUserId,
                requesterId,
                EntityType.USER,
                requesterId,
                NotificationType.FRIEND_REQUEST
        );

        evictFriendshipCache(currentUserId, requesterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FriendshipUserResponse> getFriends(
            Long userId,
            Pageable pageable
    ) {
        Long viewerId = UserContextHolder.getUserId();

        userServiceDomain.validateUserExists(userId);

        if (viewerId.equals(userId)) {
            return friendshipRepository.getMyFriends(
                    userId,
                    FriendshipStatus.ACCEPTED,
                    pageable
            );
        }

        return friendshipRepository.getFriends(
                userId,
                viewerId,
                FriendshipStatus.ACCEPTED,
                Visibility.PUBLIC,
                Visibility.FRIEND,
                FriendshipStatus.ACCEPTED,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FriendshipUserResponse> getMyFriends(
            Pageable pageable
    ) {
        Long currentUserId = UserContextHolder.getUserId();

        return friendshipRepository.getMyFriends(
                currentUserId,
                FriendshipStatus.ACCEPTED,
                pageable
        );
    }

    private void validateFriendshipTarget(
            Long currentUserId,
            Long targetUserId
    ) {
        if (currentUserId.equals(targetUserId)) {
            throw new IllegalArgumentException(
                    "You cannot perform this action with yourself"
            );
        }

        userServiceDomain.validateUserExists(targetUserId);

        if (userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)
                || userRoleServiceDomain.hasRole(
                targetUserId,
                RoleName.MODERATOR
        )) {

            throw new AccessDeniedException(
                    "You cannot send or manage friendship with this user"
            );
        }
    }

    private void evictFriendshipCache(
            Long currentUserId,
            Long targetUserId
    ) {
        userCacheService.evict(currentUserId);
        userCacheService.evict(targetUserId);
    }
}
