package media.social.modules.user.service;

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
import media.social.modules.user.service.cache.UserCacheService;
import media.social.modules.user.service.domain.UserRoleServiceDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import media.social.modules.user.service.impl.FriendshipServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceImplTest {

    @InjectMocks
    private FriendshipServiceImpl friendshipService;

    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserServiceDomain userServiceDomain;
    @Mock private UserRoleServiceDomain userRoleServiceDomain;
    @Mock private NotificationService notificationService;
    @Mock private UserCacheService userCacheService;

    // =========================================================
    // Helpers
    // =========================================================

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    // =========================================================
    // sendFriendRequest()
    // =========================================================

    @Test
    void sendFriendRequest_success() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        // userOneId = min(1,2) = 1, userTwoId = max(1,2) = 2
        User currentUser = buildUser(currentUserId);
        User targetUser  = buildUser(targetUserId);

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            // validate: không phải chính mình, user tồn tại, không phải admin/mod
            doNothing().when(userServiceDomain).validateUserExists(targetUserId);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(false);

            // chưa có friendship
            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.empty());

            when(userRepository.findById(currentUserId)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            friendshipService.sendFriendRequest(targetUserId);

            // verify lưu đúng friendship
            ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
            verify(friendshipRepository).save(captor.capture());

            Friendship saved = captor.getValue();
            assertEquals(currentUser, saved.getUserOne()); // min id = 1 = currentUser
            assertEquals(targetUser,  saved.getUserTwo());
            assertEquals(currentUser, saved.getRequester());
            assertEquals(FriendshipStatus.PENDING, saved.getStatus());

            // verify gửi notification
            verify(notificationService).create(
                    targetUser, currentUser,
                    EntityType.USER, currentUser.getId(),
                    NotificationType.FRIEND_REQUEST
            );
        }
    }

    @Test
    void sendFriendRequest_toYourself_throwsIllegalArgumentException() {
        Long currentUserId = 1L;

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            assertThrows(IllegalArgumentException.class,
                    () -> friendshipService.sendFriendRequest(currentUserId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    @Test
    void sendFriendRequest_targetIsAdmin_throwsAccessDeniedException() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(targetUserId);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(true);

            assertThrows(AccessDeniedException.class,
                    () -> friendshipService.sendFriendRequest(targetUserId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    @Test
    void sendFriendRequest_targetIsModerator_throwsAccessDeniedException() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(targetUserId);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(true);

            assertThrows(AccessDeniedException.class,
                    () -> friendshipService.sendFriendRequest(targetUserId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    @Test
    void sendFriendRequest_alreadyExists_throwsIllegalArgumentException() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(targetUserId);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(false);

            // đã có friendship rồi
            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.of(new Friendship()));

            assertThrows(IllegalArgumentException.class,
                    () -> friendshipService.sendFriendRequest(targetUserId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    // =========================================================
    // acceptFriendRequest()
    // =========================================================

    @Test
    void acceptFriendRequest_success() {
        Long currentUserId = 2L; // người nhận
        Long requesterId   = 1L; // người gửi

        // userOneId = min(1,2) = 1 = requesterId, userTwoId = max = 2 = currentUserId
        User requesterUser = buildUser(requesterId);
        User currentUser   = buildUser(currentUserId);

        Friendship friendship = Friendship.builder()
                .userOne(requesterUser)  // id=1
                .userTwo(currentUser)    // id=2
                .requester(requesterUser)
                .status(FriendshipStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.of(friendship));

            friendshipService.acceptFriendRequest(requesterId);

            assertEquals(FriendshipStatus.ACCEPTED, friendship.getStatus());
            verify(friendshipRepository).save(friendship);
            verify(userCacheService).evict(currentUserId);
            verify(userCacheService).evict(requesterId);
        }
    }

    @Test
    void acceptFriendRequest_friendshipNotFound_throwsIllegalArgumentException() {
        Long currentUserId = 2L;
        Long requesterId   = 1L;

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> friendshipService.acceptFriendRequest(requesterId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    @Test
    void acceptFriendRequest_notPendingStatus_throwsIllegalArgumentException() {
        Long currentUserId = 2L;
        Long requesterId   = 1L;

        User requesterUser = buildUser(requesterId);
        User currentUser   = buildUser(currentUserId);

        Friendship friendship = Friendship.builder()
                .userOne(requesterUser)
                .userTwo(currentUser)
                .requester(requesterUser)
                .status(FriendshipStatus.ACCEPTED) // đã accepted rồi
                .build();

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.of(friendship));

            assertThrows(IllegalArgumentException.class,
                    () -> friendshipService.acceptFriendRequest(requesterId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    @Test
    void acceptFriendRequest_wrongRequester_throwsAccessDeniedException() {
        Long currentUserId = 2L;
        Long requesterId   = 1L;

        User currentUser   = buildUser(currentUserId);
        User someOtherUser = buildUser(99L); // người gửi thực sự là user khác

        Friendship friendship = Friendship.builder()
                .userOne(someOtherUser)
                .userTwo(currentUser)
                .requester(someOtherUser) // requester.id = 99 != requesterId = 1
                .status(FriendshipStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(any(), any()))
                    .thenReturn(Optional.of(friendship));

            assertThrows(AccessDeniedException.class,
                    () -> friendshipService.acceptFriendRequest(requesterId));

            verify(friendshipRepository, never()).save(any());
        }
    }

    // =========================================================
    // rejectFriendRequest()
    // =========================================================

    @Test
    void rejectFriendRequest_success() {
        Long currentUserId = 2L; // người nhận
        Long requesterId   = 1L; // người gửi

        User requesterUser = buildUser(requesterId);
        User currentUser   = buildUser(currentUserId);

        Friendship friendship = Friendship.builder()
                .userOne(requesterUser)
                .userTwo(currentUser)
                .requester(requesterUser)
                .status(FriendshipStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.of(friendship));

            friendshipService.rejectFriendRequest(requesterId);

            verify(friendshipRepository).delete(friendship);
            verify(notificationService).delete(
                    currentUserId, requesterId,
                    EntityType.USER, requesterId,
                    NotificationType.FRIEND_REQUEST
            );
            verify(userCacheService).evict(currentUserId);
            verify(userCacheService).evict(requesterId);
        }
    }

    @Test
    void rejectFriendRequest_friendshipNotFound_throwsIllegalArgumentException() {
        Long currentUserId = 2L;
        Long requesterId   = 1L;

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> friendshipService.rejectFriendRequest(requesterId));

            verify(friendshipRepository, never()).delete(any());
        }
    }

    @Test
    void rejectFriendRequest_notPendingStatus_throwsIllegalArgumentException() {
        Long currentUserId = 2L;
        Long requesterId   = 1L;

        User requesterUser = buildUser(requesterId);
        User currentUser   = buildUser(currentUserId);

        Friendship friendship = Friendship.builder()
                .userOne(requesterUser)
                .userTwo(currentUser)
                .requester(requesterUser)
                .status(FriendshipStatus.ACCEPTED) // không còn pending
                .build();

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(1L, 2L))
                    .thenReturn(Optional.of(friendship));

            assertThrows(IllegalArgumentException.class,
                    () -> friendshipService.rejectFriendRequest(requesterId));

            verify(friendshipRepository, never()).delete(any());
        }
    }

    @Test
    void rejectFriendRequest_wrongRequester_throwsAccessDeniedException() {
        Long currentUserId = 2L;
        Long requesterId   = 1L;

        User currentUser   = buildUser(currentUserId);
        User someOtherUser = buildUser(99L);

        Friendship friendship = Friendship.builder()
                .userOne(someOtherUser)
                .userTwo(currentUser)
                .requester(someOtherUser) // requester.id = 99 != requesterId = 1
                .status(FriendshipStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            doNothing().when(userServiceDomain).validateUserExists(requesterId);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);

            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(any(), any()))
                    .thenReturn(Optional.of(friendship));

            assertThrows(AccessDeniedException.class,
                    () -> friendshipService.rejectFriendRequest(requesterId));

            verify(friendshipRepository, never()).delete(any());
        }
    }

    // =========================================================
    // getFriends() — xem friends của người khác
    // =========================================================

    @Test
    void getFriends_viewingOwnProfile_callsGetMyFriends() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        FriendshipUserResponse friend = new FriendshipUserResponse(2L, "user2", "http://avatar.url");
        Page<FriendshipUserResponse> expected = new PageImpl<>(List.of(friend));

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(viewerId);

            doNothing().when(userServiceDomain).validateUserExists(viewerId);

            when(friendshipRepository.getMyFriends(viewerId, FriendshipStatus.ACCEPTED, pageable))
                    .thenReturn(expected);

            // viewer xem chính profile mình -> viewerId == userId
            Page<FriendshipUserResponse> result = friendshipService.getFriends(viewerId, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(friendshipRepository).getMyFriends(viewerId, FriendshipStatus.ACCEPTED, pageable);
            verify(friendshipRepository, never()).getFriends(any(), any(), any(), any(), any(), any(), any());
        }
    }

    @Test
    void getFriends_viewingOtherProfile_callsGetFriends() {
        Long viewerId = 1L;
        Long targetUserId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        FriendshipUserResponse friend = new FriendshipUserResponse(3L, "user3", "http://avatar.url");
        Page<FriendshipUserResponse> expected = new PageImpl<>(List.of(friend));

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(viewerId);

            doNothing().when(userServiceDomain).validateUserExists(targetUserId);

            when(friendshipRepository.getFriends(
                    targetUserId, viewerId,
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC, Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    pageable
            )).thenReturn(expected);

            Page<FriendshipUserResponse> result = friendshipService.getFriends(targetUserId, pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(friendshipRepository).getFriends(
                    targetUserId, viewerId,
                    FriendshipStatus.ACCEPTED,
                    Visibility.PUBLIC, Visibility.FRIEND,
                    FriendshipStatus.ACCEPTED,
                    pageable
            );
            verify(friendshipRepository, never()).getMyFriends(any(), any(), any());
        }
    }

    @Test
    void getFriends_emptyList_returnsEmptyPage() {
        Long viewerId    = 1L;
        Long targetUserId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<FriendshipUserResponse> emptyPage = new PageImpl<>(List.of());

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(viewerId);

            doNothing().when(userServiceDomain).validateUserExists(targetUserId);

            when(friendshipRepository.getFriends(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(emptyPage);

            Page<FriendshipUserResponse> result = friendshipService.getFriends(targetUserId, pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // =========================================================
    // getMyFriends()
    // =========================================================

    @Test
    void getMyFriends_success() {
        Long currentUserId = 1L;
        Pageable pageable  = PageRequest.of(0, 10);

        FriendshipUserResponse friend = new FriendshipUserResponse(2L, "user2", "http://avatar.url");
        Page<FriendshipUserResponse> expected = new PageImpl<>(List.of(friend));

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(friendshipRepository.getMyFriends(currentUserId, FriendshipStatus.ACCEPTED, pageable))
                    .thenReturn(expected);

            Page<FriendshipUserResponse> result = friendshipService.getMyFriends(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            FriendshipUserResponse first = result.getContent().get(0);
            assertEquals(2L,               first.getUserId());
            assertEquals("user2",           first.getUsername());
            assertEquals("http://avatar.url", first.getAvatarUrl());

            verify(friendshipRepository).getMyFriends(currentUserId, FriendshipStatus.ACCEPTED, pageable);
        }
    }

    @Test
    void getMyFriends_emptyList_returnsEmptyPage() {
        Long currentUserId = 1L;
        Pageable pageable  = PageRequest.of(0, 10);

        Page<FriendshipUserResponse> emptyPage = new PageImpl<>(List.of());

        try (MockedStatic<UserContextHolder> ctx = mockStatic(UserContextHolder.class)) {
            ctx.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(friendshipRepository.getMyFriends(currentUserId, FriendshipStatus.ACCEPTED, pageable))
                    .thenReturn(emptyPage);

            Page<FriendshipUserResponse> result = friendshipService.getMyFriends(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void sendFriendRequest_reverseIdOrder_userOneIsTarget() {
        Long currentUserId = 10L;
        Long targetUserId = 2L;
        // currentUserId > targetUserId → userOneId=2, userTwoId=10

        User currentUser = new User();
        currentUser.setId(currentUserId);

        User targetUser = new User();
        targetUser.setId(targetUserId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(targetUserId, RoleName.MODERATOR)).thenReturn(false);
            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(targetUserId, currentUserId))
                    .thenReturn(java.util.Optional.empty());
            when(userRepository.findById(currentUserId)).thenReturn(java.util.Optional.of(currentUser));
            when(userRepository.findById(targetUserId)).thenReturn(java.util.Optional.of(targetUser));

            friendshipService.sendFriendRequest(targetUserId);

            ArgumentCaptor<Friendship> captor = ArgumentCaptor.forClass(Friendship.class);
            verify(friendshipRepository).save(captor.capture());
            Friendship saved = captor.getValue();
            // userOne should be targetUser (smaller ID), userTwo should be currentUser (larger ID)
            assertEquals(targetUserId, saved.getUserOne().getId());
            assertEquals(currentUserId, saved.getUserTwo().getId());
            assertEquals(currentUser, saved.getRequester());
            assertEquals(FriendshipStatus.PENDING, saved.getStatus());
        }
    }

    @Test
    void acceptFriendRequest_notPartOfFriendship_throwsAccessDeniedException() {
        Long currentUserId = 5L;
        Long requesterId = 1L;
        // userOneId=1, userTwoId=5

        User requester = new User();
        requester.setId(requesterId);

        // Friendship where currentUser is NOT userOne and NOT userTwo (edge case)
        User otherUser1 = new User();
        otherUser1.setId(3L);
        User otherUser2 = new User();
        otherUser2.setId(4L);

        Friendship friendship = Friendship.builder()
                .userOne(otherUser1)
                .userTwo(otherUser2)
                .requester(requester)
                .status(FriendshipStatus.PENDING)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(userRoleServiceDomain.hasRole(requesterId, RoleName.ADMIN)).thenReturn(false);
            when(userRoleServiceDomain.hasRole(requesterId, RoleName.MODERATOR)).thenReturn(false);
            when(friendshipRepository.findByUserOne_IdAndUserTwo_Id(requesterId, currentUserId))
                    .thenReturn(java.util.Optional.of(friendship));

            assertThrows(AccessDeniedException.class,
                    () -> friendshipService.acceptFriendRequest(requesterId));
            verify(friendshipRepository, never()).save(any());
        }
    }
}
