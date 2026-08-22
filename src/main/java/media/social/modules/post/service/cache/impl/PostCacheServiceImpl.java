package media.social.modules.post.service.cache.impl;

import lombok.RequiredArgsConstructor;
import media.social.infrastructure.redis.RedisCacheNames;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.PostFlatProjection;
import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.dto.response.post.PostMediaResponse;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.repository.PostMediaRepository;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.service.cache.PostCacheService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCacheServiceImpl implements PostCacheService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;

    @Override
    @Cacheable(
            value = RedisCacheNames.POSTS,
            key = "#postId + ':' + #visibilities"
    )
    @Transactional(readOnly = true)
    public PostCacheDTO getPost(
            Long postId,
            List<Visibility> visibilities
    ) {

        PostFlatProjection flat =
                postRepository.findPostDetailById(
                                postId,
                                Status.ACTIVE,
                                visibilities,
                                ReportStatus.APPROVED
                        )
                        .orElseThrow(
                                () -> new PostNotFoundException(
                                        "Post not found"
                                )
                        );

        List<PostMediaResponse> media =
                postMediaRepository.findMediaByPostId(postId)
                        .stream()
                        .map(projection ->
                                PostMediaResponse.builder()
                                        .postMediaId(
                                                projection.getPostMediaId()
                                        )
                                        .url(
                                                projection.getUrl()
                                        )
                                        .type(
                                                projection.getType()
                                        )
                                        .build()
                        )
                        .toList();

        return PostCacheDTO.builder()
                .id(flat.getId())
                .content(flat.getContent())
                .visibility(flat.getVisibility())
                .createdAt(flat.getCreatedAt())
                .userId(flat.getUserId())
                .username(flat.getUsername())
                .avatarUrl(flat.getAvatarUrl())
                .commentCount(flat.getCommentCount())
                .reactionCount(flat.getReactionCount())
                .postMediaResponses(media)
                .build();
    }

    @Override
    @CacheEvict(
            value = RedisCacheNames.POSTS,
            allEntries = true
    )
    public void evictPost(Long postId) {
        // Cache invalidation is handled by @CacheEvict.
    }
}