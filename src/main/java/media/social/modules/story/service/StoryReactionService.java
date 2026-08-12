package media.social.modules.story.service;

import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.enums.ReactionType;

public interface StoryReactionService {

    void react(
            Long storyId,
            ReactionType type
    );

    void removeReaction(
            Long storyId
    );

    ReactionCountResponse countReaction(
            Long storyId
    );

}