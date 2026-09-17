package media.social.modules.user.service.domain;

import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.repository.FriendshipRepository;
import media.social.modules.user.service.domain.impl.FriendShipDomainImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendShipDomainImplTest {

    @InjectMocks
    private FriendShipDomainImpl friendShipDomain;

    @Mock
    private FriendshipRepository friendshipRepository;

    // =========================================================
    // areFriends()
    // =========================================================

    @Test
    void areFriends_returnsTrue_whenFriendshipAccepted() {
        Long userId       = 1L;
        Long targetUserId = 2L;

        when(friendshipRepository.areFriends(userId, targetUserId, FriendshipStatus.ACCEPTED))
                .thenReturn(true);

        boolean result = friendShipDomain.areFriends(userId, targetUserId);

        assertTrue(result);
        verify(friendshipRepository).areFriends(userId, targetUserId, FriendshipStatus.ACCEPTED);
    }

    @Test
    void areFriends_returnsFalse_whenNoFriendship() {
        Long userId       = 1L;
        Long targetUserId = 2L;

        when(friendshipRepository.areFriends(userId, targetUserId, FriendshipStatus.ACCEPTED))
                .thenReturn(false);

        boolean result = friendShipDomain.areFriends(userId, targetUserId);

        assertFalse(result);
        verify(friendshipRepository).areFriends(userId, targetUserId, FriendshipStatus.ACCEPTED);
    }

    @Test
    void areFriends_alwaysPassesAcceptedStatus() {
        Long userId       = 3L;
        Long targetUserId = 4L;

        when(friendshipRepository.areFriends(userId, targetUserId, FriendshipStatus.ACCEPTED))
                .thenReturn(true);

        friendShipDomain.areFriends(userId, targetUserId);

        // Đảm bảo luôn dùng ACCEPTED chứ không phải PENDING
        verify(friendshipRepository, never())
                .areFriends(userId, targetUserId, FriendshipStatus.PENDING);
        verify(friendshipRepository).areFriends(userId, targetUserId, FriendshipStatus.ACCEPTED);
    }
}
