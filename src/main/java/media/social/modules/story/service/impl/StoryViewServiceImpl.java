package media.social.modules.story.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.story.dto.projection.StoryInteractionProjection;
import media.social.modules.story.dto.response.StoryViewResponse;
import media.social.modules.story.entity.Story;
import media.social.modules.story.entity.StoryView;
import media.social.modules.story.entity.StoryViewId;
import media.social.modules.story.exception.story.StoryForbiddenException;
import media.social.modules.story.repository.StoryViewRepository;
import media.social.modules.story.service.StoryViewService;
import media.social.modules.story.service.domain.StoryDomainService;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryViewServiceImpl implements StoryViewService {

    private final StoryViewRepository storyViewRepository;
    private final StoryDomainService storyDomainService;
    private final UserServiceDomain userServiceDomain;

    @Override
    @Transactional
    public void view(Long storyId) {

        Long userId = UserContextHolder.getUserId();

        Story story = storyDomainService.getActiveByStoryId(
                storyId
        );

        storyDomainService.checkCanViewStory(
                story,
                userId
        );

        StoryViewId viewId = new StoryViewId(
                storyId,
                userId
        );

        if (storyViewRepository.existsById(viewId)) {
            return;
        }

        User viewer = userServiceDomain.getByUserId(
                userId
        );

        StoryView storyView = StoryView.builder()
                .id(viewId)
                .story(story)
                .viewer(viewer)
                .build();

        storyViewRepository.save(storyView);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryViewResponse> getViewers(Long storyId) {
        Long currentUserId = UserContextHolder.getUserId();

        Story story = storyDomainService.getActiveByStoryId(storyId);

        if (!Objects.equals(story.getUser().getId(), currentUserId)) {
            throw new StoryForbiddenException("Chỉ chủ sở hữu tin mới có quyền xem danh sách người đã xem");
        }

        List<StoryInteractionProjection> interactions =
                storyViewRepository.findInteractionsByStoryIds(List.of(storyId));

        return interactions.stream()
                .map(i -> StoryViewResponse.builder()
                        .userId(i.getUserId())
                        .username(i.getUsername())
                        .avatarUrl(i.getAvatarUrl())
                        .viewedAt(i.getViewAt())
                        .reactionType(i.getReactionType())
                        .reactionAt(i.getReactionAt())
                        .build())
                .collect(Collectors.toList());
    }
}