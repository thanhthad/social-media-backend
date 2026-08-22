package media.social.modules.user.service.cache;

import media.social.modules.user.dto.projection.FriendshipCountProjection;
import media.social.modules.user.dto.projection.MutualFriendCountProjection;
import media.social.modules.user.dto.projection.PublicUserProfileCacheProjection;
import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.friend.FriendshipCountResponse;
import media.social.modules.user.dto.response.friend.MutualFriendCountResponse;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.FriendshipRepository;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.impl.UserProfileCacheServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileCacheServiceImpl Unit Tests")
class UserProfileCacheServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @InjectMocks
    private UserProfileCacheServiceImpl userProfileCacheService;

    private static final Long USER_ID = 1L;
    private static final Long TARGET_USER_ID = 2L;

    // -------------------------------------------------------------------------
    // evictProfile & evictFriendShipCount
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("evictProfile - does not throw")
    void evictProfile_doesNotThrow() {
        assertDoesNotThrow(() -> userProfileCacheService.evictProfile(USER_ID));
    }

    @Test
    @DisplayName("evictFriendShipCount - does not throw")
    void evictFriendShipCount_doesNotThrow() {
        assertDoesNotThrow(() -> userProfileCacheService.evictFriendShipCount(USER_ID));
    }

    // -------------------------------------------------------------------------
    // getUserProfile
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUserProfile - repository returns profile - method returns it")
    void getUserProfile_success() {
        // Arrange
        PublicUserProfileCacheProjection projection = mock(PublicUserProfileCacheProjection.class);
        when(projection.getId()).thenReturn(USER_ID);
        when(projection.getUsername()).thenReturn("janedoe");
        when(projection.getFullName()).thenReturn("Jane Doe");
        when(projection.getAvatarUrl()).thenReturn("https://cdn.example.com/avatar.png");
        when(projection.getBio()).thenReturn("Hello world!");

        when(userRepository.findCurrentUserProfileCache(USER_ID)).thenReturn(Optional.of(projection));

        // Act
        PublicUserProfileCacheResponse actual = userProfileCacheService.getUserProfile(USER_ID);

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(USER_ID);
        assertThat(actual.getUsername()).isEqualTo("janedoe");
        assertThat(actual.getFullName()).isEqualTo("Jane Doe");
        assertThat(actual.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(actual.getBio()).isEqualTo("Hello world!");
    }

    @Test
    @DisplayName("getUserProfile - repository returns empty - throws UserNotFoundException")
    void getUserProfile_notFound_throwsUserNotFoundException() {
        // Arrange
        when(userRepository.findCurrentUserProfileCache(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userProfileCacheService.getUserProfile(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    // -------------------------------------------------------------------------
    // getTotalFriend
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTotalFriend - repository returns count - method returns it")
    void getTotalFriend_success() {
        // Arrange
        FriendshipCountProjection projection = mock(FriendshipCountProjection.class);
        when(projection.getTotalFriends()).thenReturn(42L);

        when(friendshipRepository.findFriendshipCount(USER_ID, FriendshipStatus.ACCEPTED)).thenReturn(Optional.of(projection));

        // Act
        FriendshipCountResponse actual = userProfileCacheService.getTotalFriend(USER_ID);

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual.getTotalFriends()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getTotalFriend - repository returns empty - throws UserNotFoundException")
    void getTotalFriend_notFound_throwsUserNotFoundException() {
        // Arrange
        when(friendshipRepository.findFriendshipCount(USER_ID, FriendshipStatus.ACCEPTED)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userProfileCacheService.getTotalFriend(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    // -------------------------------------------------------------------------
    // getTotalMutualFriend
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getTotalMutualFriend - repository returns count - method returns it")
    void getTotalMutualFriend_success() {
        MutualFriendCountProjection projection = mock(MutualFriendCountProjection.class);
        when(projection.getTotalMutualFriends()).thenReturn(5L);

        when(friendshipRepository.findMutualFriendCount(USER_ID, TARGET_USER_ID)).thenReturn(Optional.of(projection));

        MutualFriendCountResponse actual = userProfileCacheService.getTotalMutualFriend(USER_ID, TARGET_USER_ID);

        assertThat(actual).isNotNull();
        assertThat(actual.getTotalMutualCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("getTotalMutualFriend - repository returns empty - throws UserNotFoundException")
    void getTotalMutualFriend_notFound_throwsUserNotFoundException() {
        when(friendshipRepository.findMutualFriendCount(USER_ID, TARGET_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileCacheService.getTotalMutualFriend(USER_ID, TARGET_USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }

    // -------------------------------------------------------------------------
    // getMutualFriendAvatars
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getMutualFriendAvatars - returns avatars list")
    void getMutualFriendAvatars_success() {
        List<String> avatars = List.of("https://cdn.example.com/avatar1.png", "https://cdn.example.com/avatar2.png");
        when(friendshipRepository.findMutualFriendAvatars(USER_ID, TARGET_USER_ID)).thenReturn(avatars);

        List<String> actual = userProfileCacheService.getMutualFriendAvatars(USER_ID, TARGET_USER_ID);

        assertThat(actual).isNotNull();
        assertThat(actual).isEqualTo(avatars);
    }
}
