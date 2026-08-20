package media.social.modules.dating.service.cache.impl;

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
        DatingProfileCacheResponse expectedResponse = new DatingProfileCacheResponse();
        when(datingProfileRepository.findDatingProfileCache(userId))
                .thenReturn(Optional.of(expectedResponse));

        // Act
        DatingProfileCacheResponse actualResponse = datingProfileCacheService.getDatingProfile(userId);

        // Assert
        assertThat(actualResponse).isNotNull();
        assertThat(actualResponse).isSameAs(expectedResponse);
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
