package media.social.modules.post.service.domain.impl;

import lombok.AllArgsConstructor;
import media.social.modules.post.entity.Post;
import media.social.modules.post.exception.post.ForbiddenException;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.auth.security.context.UserContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PostDomainServiceImpl implements PostDomainService {
    private final PostRepository postRepository;

    @Override
    public void validatePostExists(Long postId) {
        if(!postRepository.existsById(postId)){
            throw new PostNotFoundException("Post not found with id: " + postId);
        }
    }

    @Override
    public Post getByPostId(Long postId) {
        return postRepository.findById(postId).orElseThrow(
                () -> new PostNotFoundException("Post not found with id: " + postId)
        );
    }

    @Override
    public void checkOwner(Post post) {

        Long currentUserId = UserContextHolder.getUserId();

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You are not allowed to modify this post.");
        }
    }

    @Override
    public void increaseCommentCount(Long postId) {
        postRepository.increaseCommentCount(postId);
    }

    @Override
    public void decreaseCommentCount(Long postId) {
        postRepository.decreaseCommentCount(postId);
    }

    @Override
    public void increaseReactionCount(Long postId) {
        postRepository.increaseReactionCount(postId);
    }

    @Override
    public void decreaseReactionCount(Long postId) {
        postRepository.decreaseReactionCount(postId);
    }
}
