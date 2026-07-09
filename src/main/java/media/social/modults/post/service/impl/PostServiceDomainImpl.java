package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.post.entity.Post;
import media.social.modults.post.exception.post.ForbiddenException;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.security.context.UserContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PostServiceDomainImpl implements PostServiceDomain {
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
}
