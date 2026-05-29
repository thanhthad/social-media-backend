package media.social.modults.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.entity.Post;
import media.social.modults.user.entity.User
import media.social.modults.exception.post.PostNotFoundException;
import media.social.modults.repository.PostRepository;
import media.social.modults.service.PostServiceDomain;
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

}
