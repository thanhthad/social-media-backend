package media.social.modules.user.service;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.dto.request.block.BlockRequest;
import media.social.modules.user.dto.response.block.ListUserBlockedResponse;
import media.social.modules.user.entity.Block;
import media.social.modules.user.entity.BlockId;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.block.BlockAlreadyExistsException;
import media.social.modules.user.exception.block.BlockNotFoundException;
import media.social.modules.user.repository.BlockRepository;
import media.social.modules.user.service.domain.UserServiceDomain;
import media.social.modules.user.service.impl.BlockServiceImpl;

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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlockServiceImplTest {

    @InjectMocks
    private BlockServiceImpl blockService;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserServiceDomain userServiceDomain;

    // =========================================================
    // blockUser()
    // =========================================================

    @Test
    void blockUser_success() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        BlockRequest request = mock(BlockRequest.class);
        when(request.getBlockedId()).thenReturn(targetUserId);

        User blocker = new User();
        blocker.setId(currentUserId);

        User blocked = new User();
        blocked.setId(targetUserId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.findBlock(currentUserId, targetUserId))
                    .thenReturn(Optional.empty());
            when(userServiceDomain.getByUserId(currentUserId)).thenReturn(blocker);
            when(userServiceDomain.getByUserId(targetUserId)).thenReturn(blocked);

            blockService.blockUser(request);

            ArgumentCaptor<Block> captor = ArgumentCaptor.forClass(Block.class);
            verify(blockRepository).save(captor.capture());

            Block saved = captor.getValue();
            assertEquals(blocker, saved.getBlocker());
            assertEquals(blocked, saved.getBlocked());
            assertEquals(new BlockId(currentUserId, targetUserId), saved.getId());
        }
    }

    @Test
    void blockUser_blockYourself_throwsIllegalArgumentException() {
        Long currentUserId = 1L;

        BlockRequest request = mock(BlockRequest.class);
        when(request.getBlockedId()).thenReturn(currentUserId); // block chính mình

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            assertThrows(IllegalArgumentException.class,
                    () -> blockService.blockUser(request));

            verify(blockRepository, never()).save(any());
        }
    }

    @Test
    void blockUser_alreadyBlocked_throwsBlockAlreadyExistsException() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        BlockRequest request = mock(BlockRequest.class);
        when(request.getBlockedId()).thenReturn(targetUserId);

        Block existingBlock = new Block();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.findBlock(currentUserId, targetUserId))
                    .thenReturn(Optional.of(existingBlock));

            assertThrows(BlockAlreadyExistsException.class,
                    () -> blockService.blockUser(request));

            verify(blockRepository, never()).save(any());
        }
    }

    // =========================================================
    // unblockUser()
    // =========================================================

    @Test
    void unblockUser_success() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        BlockId expectedId = new BlockId(currentUserId, targetUserId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.existsById(expectedId)).thenReturn(true);

            blockService.unblockUser(targetUserId);

            verify(blockRepository).deleteById(expectedId);
        }
    }

    @Test
    void unblockUser_blockNotFound_throwsBlockNotFoundException() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        BlockId expectedId = new BlockId(currentUserId, targetUserId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.existsById(expectedId)).thenReturn(false);

            assertThrows(BlockNotFoundException.class,
                    () -> blockService.unblockUser(targetUserId));

            verify(blockRepository, never()).deleteById(any());
        }
    }

    // =========================================================
    // checkBlocked()
    // =========================================================

    @Test
    void checkBlocked_returnsTrue_whenBlocked() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.findBlock(currentUserId, targetUserId))
                    .thenReturn(Optional.of(new Block()));

            boolean result = blockService.checkBlocked(targetUserId);

            assertTrue(result);
        }
    }

    @Test
    void checkBlocked_returnsFalse_whenNotBlocked() {
        Long currentUserId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.findBlock(currentUserId, targetUserId))
                    .thenReturn(Optional.empty());

            boolean result = blockService.checkBlocked(targetUserId);

            assertFalse(result);
        }
    }

    // =========================================================
    // getBlockedUsers()
    // =========================================================

    @Test
    void getBlockedUsers_success_returnsPage() {
        Long currentUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        ListUserBlockedResponse blockedUser =
                new ListUserBlockedResponse(2L, "blocked_user", "Blocked Name", "http://avatar.url");

        Page<ListUserBlockedResponse> expectedPage = new PageImpl<>(List.of(blockedUser));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.findBlockedUsers(currentUserId, pageable))
                    .thenReturn(expectedPage);

            Page<ListUserBlockedResponse> result = blockService.getBlockedUsers(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());

            ListUserBlockedResponse first = result.getContent().get(0);
            assertEquals(2L, first.getId());
            assertEquals("blocked_user", first.getUsername());
            assertEquals("Blocked Name", first.getFullName());
            assertEquals("http://avatar.url", first.getAvatarUrl());

            verify(blockRepository).findBlockedUsers(currentUserId, pageable);
        }
    }

    @Test
    void getBlockedUsers_emptyList_returnsEmptyPage() {
        Long currentUserId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Page<ListUserBlockedResponse> emptyPage = new PageImpl<>(List.of());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);

            when(blockRepository.findBlockedUsers(currentUserId, pageable))
                    .thenReturn(emptyPage);

            Page<ListUserBlockedResponse> result = blockService.getBlockedUsers(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}
