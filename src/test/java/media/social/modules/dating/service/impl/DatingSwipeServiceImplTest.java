package media.social.modules.dating.service.impl;

import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.swipe.CreateDatingSwipeRequest;
import media.social.modules.dating.dto.response.swipe.DatingSwipeResponse;
import media.social.modules.dating.entity.DatingSwipe;
import media.social.modules.dating.enums.DatingSwipeAction;
import media.social.modules.dating.repository.DatingSwipeRepository;
import media.social.modules.dating.service.DatingMatchService;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatingSwipeServiceImplTest {

    @InjectMocks
    private DatingSwipeServiceImpl datingSwipeService;

    @Mock
    private DatingSwipeRepository datingSwipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DatingMatchService datingMatchService;

    // ==========================================
    // swipe() TESTS
    // ==========================================

    @Test
    void swipe_like_noMatch_success() {
        Long swiperId = 1L;
        Long targetId = 2L;

        User swiper = User.builder().id(swiperId).build();
        User target = User.builder().id(targetId).build();

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(targetId);
        request.setAction(DatingSwipeAction.LIKE);

        DatingSwipe savedSwipe = DatingSwipe.builder()
                .id(10L)
                .swiper(swiper)
                .target(target)
                .action(DatingSwipeAction.LIKE)
                .createdAt(OffsetDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(swiperId);

            when(userRepository.findById(swiperId)).thenReturn(Optional.of(swiper));
            when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(datingSwipeRepository.findBySwiperIdAndTargetId(swiperId, targetId))
                    .thenReturn(Optional.empty());
            when(datingSwipeRepository.save(any(DatingSwipe.class))).thenReturn(savedSwipe);
            when(datingSwipeRepository.existsBySwiperIdAndTargetIdAndAction(
                    targetId, swiperId, DatingSwipeAction.LIKE)).thenReturn(false);

            DatingSwipeResponse response = datingSwipeService.swipe(request);

            assertNotNull(response);
            assertEquals(targetId, response.getTargetUserId());
            assertEquals(DatingSwipeAction.LIKE, response.getAction());
            verify(datingMatchService, never()).createMatch(any(), any());
        }
    }

    @Test
    void swipe_like_withMatch_createsMatch() {
        Long swiperId = 1L;
        Long targetId = 2L;

        User swiper = User.builder().id(swiperId).build();
        User target = User.builder().id(targetId).build();

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(targetId);
        request.setAction(DatingSwipeAction.LIKE);

        DatingSwipe savedSwipe = DatingSwipe.builder()
                .id(10L)
                .swiper(swiper)
                .target(target)
                .action(DatingSwipeAction.LIKE)
                .createdAt(OffsetDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(swiperId);

            when(userRepository.findById(swiperId)).thenReturn(Optional.of(swiper));
            when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(datingSwipeRepository.findBySwiperIdAndTargetId(swiperId, targetId))
                    .thenReturn(Optional.empty());
            when(datingSwipeRepository.save(any(DatingSwipe.class))).thenReturn(savedSwipe);
            when(datingSwipeRepository.existsBySwiperIdAndTargetIdAndAction(
                    targetId, swiperId, DatingSwipeAction.LIKE)).thenReturn(true);

            DatingSwipeResponse response = datingSwipeService.swipe(request);

            assertNotNull(response);
            assertEquals(targetId, response.getTargetUserId());
            assertEquals(DatingSwipeAction.LIKE, response.getAction());
            verify(datingMatchService, times(1)).createMatch(swiper, target);
        }
    }

    @Test
    void swipe_pass_success() {
        Long swiperId = 1L;
        Long targetId = 2L;

        User swiper = User.builder().id(swiperId).build();
        User target = User.builder().id(targetId).build();

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(targetId);
        request.setAction(DatingSwipeAction.DISLIKE);

        DatingSwipe savedSwipe = DatingSwipe.builder()
                .id(11L)
                .swiper(swiper)
                .target(target)
                .action(DatingSwipeAction.DISLIKE)
                .createdAt(OffsetDateTime.now())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(swiperId);

            when(userRepository.findById(swiperId)).thenReturn(Optional.of(swiper));
            when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(datingSwipeRepository.findBySwiperIdAndTargetId(swiperId, targetId))
                    .thenReturn(Optional.empty());
            when(datingSwipeRepository.save(any(DatingSwipe.class))).thenReturn(savedSwipe);

            DatingSwipeResponse response = datingSwipeService.swipe(request);

            assertNotNull(response);
            assertEquals(targetId, response.getTargetUserId());
            assertEquals(DatingSwipeAction.DISLIKE, response.getAction());
            // For DISLIKE action, match logic must never be invoked
            verify(datingSwipeRepository, never())
                    .existsBySwiperIdAndTargetIdAndAction(any(), any(), any());
            verify(datingMatchService, never()).createMatch(any(), any());
        }
    }

    @Test
    void swipe_self_throwsIllegalArgumentException() {
        Long userId = 1L;

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(userId);
        request.setAction(DatingSwipeAction.LIKE);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> datingSwipeService.swipe(request)
            );

            assertEquals("You cannot swipe yourself", ex.getMessage());
            verify(userRepository, never()).findById(any());
            verify(datingSwipeRepository, never()).save(any());
        }
    }

    @Test
    void swipe_swiperNotFound_throwsUserNotFoundException() {
        Long swiperId = 1L;
        Long targetId = 2L;

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(targetId);
        request.setAction(DatingSwipeAction.LIKE);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(swiperId);

            when(userRepository.findById(swiperId)).thenReturn(Optional.empty());

            UserNotFoundException ex = assertThrows(
                    UserNotFoundException.class,
                    () -> datingSwipeService.swipe(request)
            );

            assertEquals("User not found", ex.getMessage());
            verify(userRepository, never()).findById(targetId);
            verify(datingSwipeRepository, never()).save(any());
        }
    }

    @Test
    void swipe_targetNotFound_throwsUserNotFoundException() {
        Long swiperId = 1L;
        Long targetId = 2L;

        User swiper = User.builder().id(swiperId).build();

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(targetId);
        request.setAction(DatingSwipeAction.LIKE);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(swiperId);

            when(userRepository.findById(swiperId)).thenReturn(Optional.of(swiper));
            when(userRepository.findById(targetId)).thenReturn(Optional.empty());

            UserNotFoundException ex = assertThrows(
                    UserNotFoundException.class,
                    () -> datingSwipeService.swipe(request)
            );

            assertEquals("Target user not found", ex.getMessage());
            verify(datingSwipeRepository, never()).save(any());
        }
    }

    @Test
    void swipe_existingSwipe_updatesAction() {
        Long swiperId = 1L;
        Long targetId = 2L;

        User swiper = User.builder().id(swiperId).build();
        User target = User.builder().id(targetId).build();

        // Existing swipe with old DISLIKE action
        DatingSwipe existingSwipe = DatingSwipe.builder()
                .id(5L)
                .swiper(swiper)
                .target(target)
                .action(DatingSwipeAction.DISLIKE)
                .createdAt(OffsetDateTime.now().minusDays(1))
                .build();

        CreateDatingSwipeRequest request = new CreateDatingSwipeRequest();
        request.setTargetUserId(targetId);
        request.setAction(DatingSwipeAction.LIKE);

        // After save, action is updated to LIKE
        DatingSwipe updatedSwipe = DatingSwipe.builder()
                .id(5L)
                .swiper(swiper)
                .target(target)
                .action(DatingSwipeAction.LIKE)
                .createdAt(existingSwipe.getCreatedAt())
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(swiperId);

            when(userRepository.findById(swiperId)).thenReturn(Optional.of(swiper));
            when(userRepository.findById(targetId)).thenReturn(Optional.of(target));
            when(datingSwipeRepository.findBySwiperIdAndTargetId(swiperId, targetId))
                    .thenReturn(Optional.of(existingSwipe));
            when(datingSwipeRepository.save(existingSwipe)).thenReturn(updatedSwipe);
            when(datingSwipeRepository.existsBySwiperIdAndTargetIdAndAction(
                    targetId, swiperId, DatingSwipeAction.LIKE)).thenReturn(false);

            DatingSwipeResponse response = datingSwipeService.swipe(request);

            assertNotNull(response);
            assertEquals(targetId, response.getTargetUserId());
            assertEquals(DatingSwipeAction.LIKE, response.getAction());
            // Verifies that the existing swipe entity was reused (save called with the same object)
            verify(datingSwipeRepository, times(1)).save(existingSwipe);
            // Action must have been mutated on the existing entity before save
            assertEquals(DatingSwipeAction.LIKE, existingSwipe.getAction());
        }
    }

    // ==========================================
    // getMySwipes() TESTS
    // ==========================================

    @Test
    void getMySwipes_success() {
        Long userId = 1L;

        User swiper = User.builder().id(userId).build();

        DatingSwipe swipe1 = DatingSwipe.builder()
                .id(1L)
                .swiper(swiper)
                .target(User.builder().id(2L).build())
                .action(DatingSwipeAction.LIKE)
                .createdAt(OffsetDateTime.now())
                .build();

        DatingSwipe swipe2 = DatingSwipe.builder()
                .id(2L)
                .swiper(swiper)
                .target(User.builder().id(3L).build())
                .action(DatingSwipeAction.DISLIKE)
                .createdAt(OffsetDateTime.now().minusHours(1))
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            when(datingSwipeRepository.findAllBySwiperIdOrderByCreatedAtDesc(userId))
                    .thenReturn(List.of(swipe1, swipe2));

            List<DatingSwipeResponse> result = datingSwipeService.getMySwipes();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(2L, result.get(0).getTargetUserId());
            assertEquals(DatingSwipeAction.LIKE, result.get(0).getAction());
            assertEquals(3L, result.get(1).getTargetUserId());
            assertEquals(DatingSwipeAction.DISLIKE, result.get(1).getAction());
        }
    }

    @Test
    void getMySwipes_empty_returnsEmpty() {
        Long userId = 1L;

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            when(datingSwipeRepository.findAllBySwiperIdOrderByCreatedAtDesc(userId))
                    .thenReturn(Collections.emptyList());

            List<DatingSwipeResponse> result = datingSwipeService.getMySwipes();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================
    // getMyLikes() TESTS
    // ==========================================

    @Test
    void getMyLikes_success() {
        Long userId = 1L;

        User swiper = User.builder().id(userId).build();
        OffsetDateTime likedAt = OffsetDateTime.now();

        DatingSwipe likeSwipe = DatingSwipe.builder()
                .id(1L)
                .swiper(swiper)
                .target(User.builder().id(2L).build())
                .action(DatingSwipeAction.LIKE)
                .createdAt(likedAt)
                .build();

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            when(datingSwipeRepository.findAllBySwiperIdAndActionOrderByCreatedAtDesc(
                    userId, DatingSwipeAction.LIKE))
                    .thenReturn(List.of(likeSwipe));

            List<DatingSwipeResponse> result = datingSwipeService.getMyLikes();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(2L, result.get(0).getTargetUserId());
            assertEquals(DatingSwipeAction.LIKE, result.get(0).getAction());
            assertEquals(likedAt, result.get(0).getCreatedAt());
        }
    }

    @Test
    void getMyLikes_empty_returnsEmpty() {
        Long userId = 1L;

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            when(datingSwipeRepository.findAllBySwiperIdAndActionOrderByCreatedAtDesc(
                    userId, DatingSwipeAction.LIKE))
                    .thenReturn(Collections.emptyList());

            List<DatingSwipeResponse> result = datingSwipeService.getMyLikes();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================
    // deleteSwipe() TESTS
    // ==========================================

    @Test
    void deleteSwipe_success() {
        Long userId = 1L;
        Long targetUserId = 2L;

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(userId);

            doNothing().when(datingSwipeRepository).deleteBySwiperIdAndTargetId(userId, targetUserId);

            datingSwipeService.deleteSwipe(targetUserId);

            verify(datingSwipeRepository, times(1))
                    .deleteBySwiperIdAndTargetId(userId, targetUserId);
        }
    }
}
