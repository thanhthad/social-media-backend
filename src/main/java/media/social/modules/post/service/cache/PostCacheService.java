package media.social.modules.post.service.cache;

import lombok.RequiredArgsConstructor;
import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.dto.response.post.PostFlatResponse;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.repository.PostMediaRepository;
import media.social.modules.post.repository.PostRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;

    @Cacheable(
            value = "posts",
            key = "#postId"
    )
    @Transactional(readOnly = true)
    public PostCacheDTO getPost(
            Long postId,
            List<Visibility> visibilities
    ){

        PostFlatResponse flat =
                postRepository.findPostDetailById(
                                postId,
                                visibilities
                        )
                        .orElseThrow(
                                () -> new PostNotFoundException(
                                        "Post not found"
                                )
                        );
        return PostCacheDTO.builder()
                .id(flat.getId())
                .content(flat.getContent())
                .visibility(flat.getVisibility())
                .createdAt(flat.getCreatedAt())
                .userId(flat.getUserId())
                .username(flat.getUsername())
                .avatarUrl(flat.getAvatarUrl())
                .commentCount(
                        flat.getCommentCount()
                )
                .reactionCount(
                        flat.getReactionCount()
                )
                .postMediaResponses(
                        postMediaRepository
                                .findMediaResponseByPostId(postId)
                )
                .build();
    }

}