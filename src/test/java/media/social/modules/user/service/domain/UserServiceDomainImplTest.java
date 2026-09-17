package media.social.modules.user.service.domain;

import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import media.social.modules.user.service.domain.impl.UserServiceDomainImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceDomainImplTest {

    @InjectMocks
    private UserServiceDomainImpl userServiceDomain;

    @Mock
    private UserRepository userRepository;

    @Test
    void validateUserExists_success() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(true);

        assertDoesNotThrow(() -> userServiceDomain.validateUserExists(userId));
        verify(userRepository).existsById(userId);
    }

    @Test
    void validateUserExists_userNotFound_throwsUserNotFoundException() {
        Long userId = 1L;
        when(userRepository.existsById(userId)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> userServiceDomain.validateUserExists(userId));
    }

    @Test
    void getByUserId_success() {
        Long userId = 1L;
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userServiceDomain.getByUserId(userId);

        assertNotNull(result);
        assertSame(user, result);
    }

    @Test
    void getByUserId_notFound_throwsUserNotFoundException() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userServiceDomain.getByUserId(userId));
    }

    @Test
    void getByEmail_success() {
        String email = "test@example.com";
        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User result = userServiceDomain.getByEmail(email);

        assertNotNull(result);
        assertSame(user, result);
    }

    @Test
    void getByEmail_notFound_throwsUserNotFoundException() {
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userServiceDomain.getByEmail(email));
    }

    @Test
    void increaseFailedAttempt_below5_incrementsAttempt() {
        String email = "test@example.com";
        User user = new User();
        user.setFailedAttempt(3);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        userServiceDomain.increaseFailedAttempt(email);

        assertEquals(4, user.getFailedAttempt());
        assertNull(user.getLockUntil());
        verify(userRepository).save(user);
    }

    @Test
    void increaseFailedAttempt_reaches5_setsLockUntilAndResetsAttempt() {
        String email = "test@example.com";
        User user = new User();
        user.setFailedAttempt(4);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        userServiceDomain.increaseFailedAttempt(email);

        assertEquals(0, user.getFailedAttempt());
        assertNotNull(user.getLockUntil());
        assertTrue(user.getLockUntil().isAfter(LocalDateTime.now()));
        verify(userRepository).save(user);
    }

    @Test
    void resetFailedAttempt_success() {
        Long userId = 1L;
        User user = new User();
        user.setFailedAttempt(3);
        user.setLockUntil(LocalDateTime.now().plusHours(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userServiceDomain.resetFailedAttempt(userId);

        assertEquals(0, user.getFailedAttempt());
        assertNull(user.getLockUntil());
        verify(userRepository).save(user);
    }

    @Test
    void isAccountLocked_returnsTrue_whenLockUntilIsInFuture() {
        User user = new User();
        user.setLockUntil(LocalDateTime.now().plusMinutes(5));

        boolean result = userServiceDomain.isAccountLocked(user);

        assertTrue(result);
    }

    @Test
    void isAccountLocked_returnsFalse_whenLockUntilIsNull() {
        User user = new User();
        user.setLockUntil(null);

        boolean result = userServiceDomain.isAccountLocked(user);

        assertFalse(result);
    }

    @Test
    void isAccountLocked_returnsFalse_whenLockUntilIsInPast() {
        User user = new User();
        user.setLockUntil(LocalDateTime.now().minusMinutes(5));

        boolean result = userServiceDomain.isAccountLocked(user);

        assertFalse(result);
    }
}
