package media.social.modules.dating.service.cache.impl;

import media.social.modules.dating.dto.projection.DatingProfileCacheProjection;
import media.social.modules.dating.dto.response.cache.DatingProfileCacheResponse;
import media.social.modules.dating.exception.profile.DatingProfileNotFoundException;
import media.social.modules.dating.repository.DatingProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingProfileCacheServiceImplTest {

    @Mock
    private DatingProfileRepository datingProfileRepository;

    @InjectMocks
    private DatingProfileCacheServiceImpl datingProfileCacheService;

    // -------------------------------------------------------------------------
    // getDatingProfile
    // -------------------------------------------------------------------------

    @Test
    void getDatingProfile_found_returnsCacheResponse() {
        // Arrange
        Long userId = 1L;
        DatingProfileCacheProjection projection = mock(DatingProfileCacheProjection.class);
        when(projection.getUsername()).thenReturn("alice");
        when(projection.getDisplayName()).thenReturn("Alice");

        when(datingProfileRepository.findDatingProfileCache(userId))
                .thenReturn(Optional.of(projection));

        // Act
        DatingProfileCacheResponse actualResponse = datingProfileCacheService.getDatingProfile(userId);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse.getUsername()).isEqualTo("alice");
        assertThat(actualResponse.getDisplayName()).isEqualTo("Alice");
        verify(datingProfileRepository, times(1)).findDatingProfileCache(userId);
    }

    @Test
    void getDatingProfile_notFound_throwsDatingProfileNotFoundException() {
        // Arrange
        Long userId = 99L;
        when(datingProfileRepository.findDatingProfileCache(userId))
                .thenReturn(Optional.empty());

        // Act & Assert
        DatingProfileNotFoundException exception = assertThrows(
                DatingProfileNotFoundException.class,
                () -> datingProfileCacheService.getDatingProfile(userId)
        );

        assertThat(exception.getMessage()).isEqualTo("Dating profile not found");
        verify(datingProfileRepository, times(1)).findDatingProfileCache(userId);
    }

    // -------------------------------------------------------------------------
    // evictProfile
    // -------------------------------------------------------------------------

    @Test
    void evictProfile_doesNothing_noException() {
        // Arrange
        Long userId = 1L;

        // Act & Assert
        assertDoesNotThrow(() -> datingProfileCacheService.evictProfile(userId));
        verifyNoInteractions(datingProfileRepository);
    }
}
