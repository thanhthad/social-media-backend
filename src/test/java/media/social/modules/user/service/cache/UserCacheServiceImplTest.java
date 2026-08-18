package media.social.modules.user.service.cache;

import media.social.modules.user.dto.response.cache.UserCacheResponse;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.cache.impl.UserCacheServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCacheServiceImpl Unit Tests")
class UserCacheServiceImplTest {

    @Mock
    private CacheManager cacheManager;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserCacheServiceImpl userCacheService;

    private static final Long USER_ID = 1L;

    // -------------------------------------------------------------------------
    // evictProfile
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("evictProfile - does not throw")
    void evictProfile_doesNotThrow() {
        // @CacheEvict is annotation-driven and has no effect in a plain unit test;
        // the method body is empty, so it must complete without any exception.
        assertDoesNotThrow(() -> userCacheService.evictProfile(USER_ID));
    }

    // -------------------------------------------------------------------------
    // evict
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("evict - does not throw")
    void evict_doesNotThrow() {
        // Same reasoning as evictProfile – empty body, no Spring context needed.
        assertDoesNotThrow(() -> userCacheService.evict(USER_ID));
    }

    // -------------------------------------------------------------------------
    // getUser
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getUser - user found - returns correctly mapped UserCacheResponse")
    void getUser_success() {
        // Arrange
        var profile = mock(media.social.modules.user.entity.Profile.class);
        when(profile.getFullName()).thenReturn("Jane Doe");
        when(profile.getAvatarUrl()).thenReturn("https://cdn.example.com/avatar.png");

        var user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getUsername()).thenReturn("janedoe");
        when(user.getProfile()).thenReturn(profile);

        when(userRepository.findByIdWithProfile(USER_ID)).thenReturn(Optional.of(user));

        // Act
        UserCacheResponse response = userCacheService.getUser(USER_ID);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getUsername()).isEqualTo("janedoe");
        assertThat(response.getFullname()).isEqualTo("Jane Doe");
        assertThat(response.getAvatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
    }

    @Test
    @DisplayName("getUser - user not found - throws UserNotFoundException")
    void getUser_userNotFound_throwsUserNotFoundException() {
        // Arrange
        when(userRepository.findByIdWithProfile(USER_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userCacheService.getUser(USER_ID))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User not found");
    }
}
