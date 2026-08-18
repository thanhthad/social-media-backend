package media.social.modules.user.service;

import media.social.modules.auth.Enum.RoleName;
import media.social.modules.auth.entity.RefreshToken;
import media.social.modules.auth.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modules.auth.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modules.auth.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modules.auth.repository.RefreshTokenRepository;
import media.social.modules.auth.security.jwt.JwtUtil;
import media.social.modules.user.entity.Role;
import media.social.modules.user.entity.User;
import media.social.modules.user.entity.UserRole;
import media.social.modules.user.service.domain.UserServiceDomain;
import media.social.modules.user.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    @Test
    void create_success() {
        Long userId = 1L;
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userId);
        when(userServiceDomain.getByUserId(userId)).thenReturn(mockUser);

        // Inject refreshExpirationMs value
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 3600000L);

        RefreshToken mockSavedToken = new RefreshToken();
        mockSavedToken.setId(10L);
        mockSavedToken.setToken("generated-token-uuid");
        mockSavedToken.setUser(mockUser);
        
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mockSavedToken);

        RefreshToken result = refreshTokenService.create(userId);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("generated-token-uuid", result.getToken());
        assertEquals(mockUser, result.getUser());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void verify_success() {
        String tokenStr = "valid-token";
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiredAt(LocalDateTime.now().plusHours(1));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.verify(tokenStr);

        assertNotNull(result);
        assertSame(token, result);
    }

    @Test
    void verify_nullToken_throwsInvalidRefreshTokenException() {
        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.verify(null));
    }

    @Test
    void verify_blankToken_throwsInvalidRefreshTokenException() {
        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.verify("  "));
    }

    @Test
    void verify_tokenNotFound_throwsInvalidRefreshTokenException() {
        String tokenStr = "nonexistent-token";
        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.verify(tokenStr));
    }

    @Test
    void verify_revokedToken_throwsRefreshTokenRevokedException() {
        String tokenStr = "revoked-token";
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(RefreshTokenRevokedException.class, () -> refreshTokenService.verify(tokenStr));
    }

    @Test
    void verify_expiredToken_throwsRefreshTokenExpiredException() {
        String tokenStr = "expired-token";
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiredAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        assertThrows(RefreshTokenExpiredException.class, () -> refreshTokenService.verify(tokenStr));
    }

    @Test
    void findValidByUser_existingValidToken_returnsExisting() {
        Long userId = 1L;
        RefreshToken existingToken = new RefreshToken();
        
        when(refreshTokenRepository.findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(Optional.of(existingToken));

        RefreshToken result = refreshTokenService.findValidByUser(userId);

        assertNotNull(result);
        assertSame(existingToken, result);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void findValidByUser_noValidToken_createsNewToken() {
        Long userId = 1L;
        User mockUser = mock(User.class);
        
        when(refreshTokenRepository.findFirstByUserIdAndRevokedFalseAndExpiredAtAfterOrderByExpiredAtDesc(
                eq(userId), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());
        when(userServiceDomain.getByUserId(userId)).thenReturn(mockUser);
        
        RefreshToken mockSavedToken = new RefreshToken();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(mockSavedToken);

        RefreshToken result = refreshTokenService.findValidByUser(userId);

        assertNotNull(result);
        assertSame(mockSavedToken, result);
    }

    @Test
    void generateAccessToken_success() {
        String tokenStr = "valid-token";
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiredAt(LocalDateTime.now().plusHours(1));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getUsername()).thenReturn("username");
        when(mockUser.getEmail()).thenReturn("email@test.com");

        UserRole userRole = mock(UserRole.class);
        Role role = mock(Role.class);
        when(role.getName()).thenReturn(RoleName.USER);
        when(userRole.getRole()).thenReturn(role);
        when(mockUser.getUserRoles()).thenReturn(Set.of(userRole));

        token.setUser(mockUser);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));
        when(jwtUtil.generateAccessToken(eq(1L), eq("username"), eq("email@test.com"), anyCollection()))
                .thenReturn("access-token-string");

        String result = refreshTokenService.generateAccessToken(tokenStr);

        assertEquals("access-token-string", result);
    }

    @Test
    void revoke_success() {
        String tokenStr = "valid-token";
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiredAt(LocalDateTime.now().plusHours(1));

        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        token.setUser(mockUser);

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(token));

        refreshTokenService.revoke(tokenStr);

        assertTrue(token.isRevoked());
    }
}
