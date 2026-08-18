package media.social.modules.user.service.domain;

import media.social.modules.user.repository.BlockRepository;
import media.social.modules.user.service.domain.impl.BlockPolicyServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockPolicyServiceImplTest {

    @InjectMocks
    private BlockPolicyServiceImpl blockPolicyService;

    @Mock
    private BlockRepository blockRepository;

    // =========================================================
    // isBlocked()
    // =========================================================

    @Test
    void isBlocked_returnsFalse_whenViewerIdIsNull() {
        boolean result = blockPolicyService.isBlocked(null, 2L);

        assertFalse(result);
        verifyNoInteractions(blockRepository);
    }

    @Test
    void isBlocked_returnsFalse_whenTargetIdIsNull() {
        boolean result = blockPolicyService.isBlocked(1L, null);

        assertFalse(result);
        verifyNoInteractions(blockRepository);
    }

    @Test
    void isBlocked_returnsFalse_whenBothIdsNull() {
        boolean result = blockPolicyService.isBlocked(null, null);

        assertFalse(result);
        verifyNoInteractions(blockRepository);
    }

    @Test
    void isBlocked_returnsTrue_whenViewerBlockedTarget() {
        Long viewerId = 1L;
        Long targetId = 2L;

        // viewer đã block target
        when(blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId)).thenReturn(true);

        boolean result = blockPolicyService.isBlocked(viewerId, targetId);

        assertTrue(result);
    }

    @Test
    void isBlocked_returnsTrue_whenTargetBlockedViewer() {
        Long viewerId = 1L;
        Long targetId = 2L;

        // target đã block viewer
        when(blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId)).thenReturn(false);
        when(blockRepository.existsByBlockerIdAndBlockedId(targetId, viewerId)).thenReturn(true);

        boolean result = blockPolicyService.isBlocked(viewerId, targetId);

        assertTrue(result);
    }

    @Test
    void isBlocked_returnsFalse_whenNeitherBlocked() {
        Long viewerId = 1L;
        Long targetId = 2L;

        when(blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId)).thenReturn(false);
        when(blockRepository.existsByBlockerIdAndBlockedId(targetId, viewerId)).thenReturn(false);

        boolean result = blockPolicyService.isBlocked(viewerId, targetId);

        assertFalse(result);
    }

    @Test
    void isBlocked_returnsTrue_whenBothBlockedEachOther() {
        Long viewerId = 1L;
        Long targetId = 2L;

        when(blockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId)).thenReturn(true);

        boolean result = blockPolicyService.isBlocked(viewerId, targetId);

        assertTrue(result);
    }
}
