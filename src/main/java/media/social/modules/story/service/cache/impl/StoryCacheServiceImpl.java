package media.social.modules.story.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.Enum.Status;
import media.social.modules.story.dto.projection.UserStoryProjection;
import media.social.modules.story.dto.response.UserStoryResponse;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.story.service.cache.StoryCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryCacheServiceImpl implements StoryCacheService {

    private final StoryRepository storyRepository;

    @Override
    @Cacheable(
            value = "userStories",
            key = "#targetUserId"
    )
    @Transactional(readOnly = true)
    public List<UserStoryResponse> getUserStories(
            Long targetUserId
    ) {

        List<UserStoryProjection> projections =
                storyRepository.findActiveStoriesByUserId(
                        targetUserId,
                        LocalDateTime.now(),
                        Status.ACTIVE
                );

        return projections.stream()
                .map(row -> UserStoryResponse.builder()
                        .storyId(row.getStoryId())
                        .userId(row.getUserId())
                        .username(row.getUsername())
                        .avatarUrl(row.getAvatarUrl())
                        .content(row.getContent())
                        .visibility(row.getVisibility())
                        .mediaType(row.getMediaType())
                        .url(row.getUrl())
                        .expiresAt(row.getExpiresAt())
                        .createdAt(row.getCreatedAt())
                        .updatedAt(row.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    @CacheEvict(
            value = "userStories",
            key = "#userId"
    )
    public void evictUserStories(Long userId) {
    }
}