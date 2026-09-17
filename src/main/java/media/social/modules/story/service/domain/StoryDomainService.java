package media.social.modules.story.service.domain;

import lombok.RequiredArgsConstructor;
import media.social.modules.post.enums.Visibility;
import media.social.modules.story.entity.Story;
import media.social.modules.story.exception.story.StoryAccessDeniedException;
import media.social.modules.story.exception.story.StoryNotFoundException;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.user.service.domain.FriendShipDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryDomainService {

    private final StoryRepository storyRepository;
    private final FriendShipDomain friendShipDomain;

    public Story getByStoryId(Long storyId) {

        return storyRepository.findById(storyId)
                .orElseThrow(
                        () -> new StoryNotFoundException(
                                "Story not found: " + storyId
                        )
                );
    }

    public Story getActiveByStoryId(Long storyId) {

        return storyRepository
                .findByIdAndExpiresAtAfter(
                        storyId,
                        LocalDateTime.now()
                )
                .orElseThrow(
                        () -> new StoryNotFoundException(
                                "Story not found or expired: " + storyId
                        )
                );
    }

    public void checkCanViewStory(
            Story story,
            Long userId
    ) {

        if (story.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new StoryNotFoundException(
                    "Story has expired"
            );
        }

        Long ownerId = story.getUser().getId();

        if (ownerId.equals(userId)) {
            return;
        }
        Visibility visibility = story.getVisibility();
        if (visibility == Visibility.PUBLIC) {
            return;
        }
        if (visibility == Visibility.PRIVATE) {
            throw new StoryAccessDeniedException(
                    "You do not have permission to view this story"
            );
        }
        if (visibility == Visibility.FRIEND) {

            boolean isFriend =
                    friendShipDomain.areFriends(
                            ownerId,
                            userId
                    );

            if (!isFriend) {
                throw new StoryAccessDeniedException(
                        "You must be friends with the story owner"
                );
            }

            return;
        }

        throw new StoryAccessDeniedException(
                "You do not have permission to view this story"
        );
    }

    public void checkOwner(
            Story story,
            Long userId
    ) {

        if (!story.getUser().getId().equals(userId)) {
            throw new StoryAccessDeniedException(
                    "You are not the owner of this story"
            );
        }
    }
}