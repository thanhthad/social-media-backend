package media.social.modules.dating.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.dating.dto.request.swipe.CreateDatingSwipeRequest;
import media.social.modules.dating.dto.response.swipe.DatingSwipeResponse;
import media.social.modules.dating.entity.DatingSwipe;
import media.social.modules.dating.enums.DatingSwipeAction;
import media.social.modules.dating.repository.DatingSwipeRepository;
import media.social.modules.dating.service.DatingMatchService;
import media.social.modules.dating.service.DatingSwipeService;
import media.social.modules.user.entity.User;
import media.social.modules.user.exception.user.UserNotFoundException;
import media.social.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DatingSwipeServiceImpl implements DatingSwipeService {

    private final DatingSwipeRepository datingSwipeRepository;
    private final UserRepository userRepository;
    private final DatingMatchService datingMatchService;

    @Override
    public DatingSwipeResponse swipe(CreateDatingSwipeRequest request) {
        Long swiperId = UserContextHolder.getUserId();

        if (swiperId.equals(request.getTargetUserId())) {
            throw new IllegalArgumentException("You cannot swipe yourself");
        }

        User swiper = userRepository.findById(swiperId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        User target = userRepository.findById(request.getTargetUserId())
                .orElseThrow(() -> new UserNotFoundException("Target user not found"));

        DatingSwipe swipe = datingSwipeRepository
                .findBySwiperIdAndTargetId(swiperId, request.getTargetUserId())
                .orElseGet(() -> DatingSwipe.builder()
                        .swiper(swiper)
                        .target(target)
                        .build());

        if(datingSwipeRepository.existsBySwiperIdAndTargetId(request.getTargetUserId(),swiperId)){
            datingMatchService.createMatch(swiper,target);
        }

        swipe.setAction(request.getAction());

        DatingSwipe savedSwipe = datingSwipeRepository.save(swipe);

        return mapToResponse(savedSwipe);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatingSwipeResponse> getMySwipes() {
        Long userId = UserContextHolder.getUserId();

        return datingSwipeRepository
                .findAllBySwiperIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DatingSwipeResponse> getMyLikes() {
        Long userId = UserContextHolder.getUserId();

        return datingSwipeRepository
                .findAllBySwiperIdAndActionOrderByCreatedAtDesc(
                        userId,
                        DatingSwipeAction.LIKE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteSwipe(Long targetUserId) {
        Long userId = UserContextHolder.getUserId();

        datingSwipeRepository.deleteBySwiperIdAndTargetId(
                userId,
                targetUserId
        );
    }

    private DatingSwipeResponse mapToResponse(DatingSwipe swipe) {
        return DatingSwipeResponse.builder()
                .targetUserId(swipe.getTarget().getId())
                .action(swipe.getAction())
                .createdAt(swipe.getCreatedAt())
                .build();
    }
}