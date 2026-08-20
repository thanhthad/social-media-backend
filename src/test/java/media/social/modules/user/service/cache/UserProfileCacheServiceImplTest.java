package media.social.modules.user.service.cache;

import media.social.modules.user.dto.response.cache.PublicUserProfileCacheResponse;
import media.social.modules.user.dto.response.friend.FriendshipCountResponse;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.impl.UserProfileCacheServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileCacheServiceImpl Unit Tests")
class UserProfileCacheServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProfileCacheServiceImpl userProfileCacheService;

    private static final Long USER_ID = 1L;

    // -------------------------------------------------------------------------
    // getUserProfile
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUserProfile - repository returns profile - method returns it")
    void getUserProfile_success() {
        // Arrange
        PublicUserProfileCacheResponse expected = PublicUserProfileCacheResponse.builder()
                .id(USER_ID)
                .username("janedoe")
                .fullName("Jane Doe")
                .avatarUrl("https://cdn.example.com/avatar.png")
                .bio("Hello world!")
                .build();

        when(userRepository.findCurrentUserProfileCache(USER_ID)).thenReturn(Optional.of(expected));

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
        FriendshipCountResponse expected = FriendshipCountResponse.builder()
                .totalFriends(42L)
                .build();

        when(userRepository.findFriendshipCount(USER_ID)).thenReturn(Optional.of(expected));

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
        when(userRepository.findFriendshipCount(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userProfileCacheService.getTotalFriend(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }
}
