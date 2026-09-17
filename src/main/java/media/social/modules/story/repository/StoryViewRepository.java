package media.social.modules.story.repository;

import media.social.modules.story.dto.projection.StoryInteractionProjection;
import media.social.modules.story.entity.StoryView;
import media.social.modules.story.entity.StoryViewId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StoryViewRepository
        extends JpaRepository<StoryView, StoryViewId> {

    @Query("""
        SELECT
            sv.story.id AS storyId,
            u.id AS userId,
            u.username AS username,
            p.avatarUrl AS avatarUrl,
            sv.viewedAt AS viewAt,
            sr.type AS reactionType,
            sr.createdAt AS reactionAt
        FROM StoryView sv
        JOIN sv.viewer u
        LEFT JOIN u.profile p
        LEFT JOIN StoryReaction sr
            ON sr.story.id = sv.story.id
            AND sr.user.id = u.id
        WHERE sv.story.id IN :storyIds
        ORDER BY sv.viewedAt DESC
        """)
    List<StoryInteractionProjection> findInteractionsByStoryIds(
            @Param("storyIds") List<Long> storyIds
    );
}