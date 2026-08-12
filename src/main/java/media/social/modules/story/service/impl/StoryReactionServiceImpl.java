package media.social.modules.story.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.story.entity.Story;
import media.social.modules.story.entity.StoryReaction;
import media.social.modules.story.exception.reaction.StoryReactionNotFoundException;
import media.social.modules.story.repository.StoryReactionRepository;
import media.social.modules.story.service.StoryReactionService;
import media.social.modules.story.service.domain.StoryDomainService;
import media.social.modules.user.entity.User;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class StoryReactionServiceImpl implements StoryReactionService {

    private final StoryReactionRepository storyReactionRepository;
    private final StoryDomainService storyDomainService;
    private final UserServiceDomain userServiceDomain;

    @Override
    @Transactional
    public void react(
            Long storyId,
            ReactionType type
    ) {

        Long userId = UserContextHolder.getUserId();

        Story story = storyDomainService.getByStoryId(storyId);

        storyDomainService.checkCanViewStory(
                story,
                userId
        );

        User user = userServiceDomain.getByUserId(userId);

        StoryReaction reaction = storyReactionRepository
                .findByUserIdAndStoryId(userId, storyId)
                .orElse(null);

        if (reaction == null) {

            reaction = StoryReaction.builder()
                    .story(story)
                    .user(user)
                    .type(type)
                    .build();

            storyReactionRepository.save(reaction);

            return;
        }

        if (reaction.getType() != type) {

            reaction.setType(type);

            storyReactionRepository.save(reaction);
        }
    }

    @Override
    @Transactional
    public void removeReaction(
            Long storyId
    ) {

        Long userId = UserContextHolder.getUserId();

        storyDomainService.getByStoryId(storyId);

        StoryReaction reaction = storyReactionRepository
                .findByUserIdAndStoryId(userId, storyId)
                .orElseThrow(
                        () -> new StoryReactionNotFoundException(
                                "Story reaction not found"
                        )
                );

        storyReactionRepository.delete(reaction);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionCountResponse countReaction(
            Long storyId
    ) {

        storyDomainService.getByStoryId(storyId);

        Map<ReactionType, Long> counts =
                new EnumMap<>(ReactionType.class);

        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }

        List<Object[]> results =
                storyReactionRepository
                        .countReactionTypesByStoryId(storyId);

        for (Object[] row : results) {

            ReactionType type = (ReactionType) row[0];

            Long count = (Long) row[1];

            counts.put(type, count);
        }

        return ReactionCountResponse.builder()
                .counts(counts)
                .build();
    }


}