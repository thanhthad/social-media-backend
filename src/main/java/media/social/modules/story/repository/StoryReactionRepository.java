package media.social.modules.story.repository;

import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.story.entity.StoryReaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoryReactionRepository extends JpaRepository<StoryReaction, Long> {

    Optional<StoryReaction> findByUserIdAndStoryId(
            Long userId,
            Long storyId
    );

    @Query("""
            SELECT r.type, COUNT(r)
            FROM StoryReaction r
            WHERE r.story.id = :storyId
            GROUP BY r.type
            """)
    List<Object[]> countReactionTypesByStoryId(
            @Param("storyId") Long storyId
    );
}