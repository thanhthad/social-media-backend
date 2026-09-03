package media.social.modules.user.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.enums.Visibility;
import media.social.modules.user.dto.response.friend.FriendSuggestionResponse;
import media.social.modules.user.dto.response.friend.MutualFriendResponse;
import media.social.modules.user.dto.response.user.FriendshipUserResponse;
import media.social.modules.user.entity.Friendship;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.repository.FriendshipRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.FriendshipService;
import media.social.modules.user.service.cache.UserProfileCacheService;
import media.social.modules.user.service.domain.UserRoleServiceDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipServiceImpl implements FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserServiceDomain userServiceDomain;
    private final UserRoleServiceDomain userRoleServiceDomain;
    private final NotificationService notificationService;
    private final UserProfileCacheService userCacheService;

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

        User currentUser = userRepository.findById(currentUserId)
                .orElse(null);
        User requester = userRepository.findById(requesterId)
                .orElse(null);

        if (currentUser != null && requester != null) {
            notificationService.create(
                    requester,
                    currentUser,
                    EntityType.USER,
                    currentUser.getId(),
                    NotificationType.FRIEND_ACCEPTED
            );
        }

        userCacheService.evictProfile(currentUserId);
        userCacheService.evictProfile(requesterId);
        userCacheService.evictFriendShipCount(requesterId);
        userCacheService.evictFriendShipCount(currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FriendshipUserResponse> getPendingFriendRequests(
            Pageable pageable
    ) {
        Long currentUserId = UserContextHolder.getUserId();

        return friendshipRepository
                .getPendingFriendRequests(
                        currentUserId,
                        FriendshipStatus.PENDING,
                        pageable
                )
                .map(projection ->
                        new FriendshipUserResponse(
                                projection.getUserId(),
                                projection.getUsername(),
                                projection.getAvatarUrl()
                        )
                );
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

        userCacheService.evictProfile(currentUserId);
        userCacheService.evictProfile(requesterId);
        userCacheService.evictFriendShipCount(requesterId);
        userCacheService.evictFriendShipCount(currentUserId);
    }

    @Override
    @Transactional
    public void cancelFriendRequest(Long targetUserId) {

        Long currentUserId = UserContextHolder.getUserId();

        validateFriendshipTarget(
                currentUserId,
                targetUserId
        );

        Long userOneId = Math.min(
                currentUserId,
                targetUserId
        );

        Long userTwoId = Math.max(
                currentUserId,
                targetUserId
        );

        Friendship friendship = friendshipRepository
                .findByUserOne_IdAndUserTwo_Id(
                        userOneId,
                        userTwoId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Friend request not found"
                        )
                );

        if (!friendship.getRequester()
                .getId()
                .equals(currentUserId)) {

            throw new AccessDeniedException(
                    "You can only cancel a friend request sent by yourself"
            );
        }

        if (friendship.getStatus()
                != FriendshipStatus.PENDING) {

            throw new IllegalArgumentException(
                    "Only pending friend requests can be cancelled"
            );
        }

        friendship.setStatus(
                FriendshipStatus.CANCELLED
        );

        friendshipRepository.save(friendship);

        notificationService.delete(
                targetUserId,
                currentUserId,
                EntityType.USER,
                currentUserId,
                NotificationType.FRIEND_REQUEST
        );

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
            return friendshipRepository
                    .getMyFriends(
                            userId,
                            FriendshipStatus.ACCEPTED,
                            pageable
                    )
                    .map(projection ->
                            new FriendshipUserResponse(
                                    projection.getUserId(),
                                    projection.getUsername(),
                                    projection.getAvatarUrl()
                            )
                    );
        }

        return friendshipRepository
                .getFriends(
                        userId,
                        viewerId,
                        FriendshipStatus.ACCEPTED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        FriendshipStatus.ACCEPTED,
                        pageable
                )
                .map(projection ->
                        new FriendshipUserResponse(
                                projection.getUserId(),
                                projection.getUsername(),
                                projection.getAvatarUrl()
                        )
                );
    }


    @Override
    @Transactional(readOnly = true)
    public List<FriendSuggestionResponse> getSuggestedUsers() {

        Long currentUserId = UserContextHolder.getUserId();

        List<Object[]> results =
                friendshipRepository.findSuggestedUsers(currentUserId);

        return results.stream()
                .map(row -> new FriendSuggestionResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MutualFriendResponse> getMutualFriends(
            Long targetUserId
    ) {
        Long currentUserId = UserContextHolder.getUserId();

        userServiceDomain.validateUserExists(targetUserId);

        List<Object[]> results =
                friendshipRepository.findMutualFriends(
                        currentUserId,
                        targetUserId
                );

        return results.stream()
                .map(row -> new MutualFriendResponse(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (String) row[2]
                ))
                .toList();
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

}
