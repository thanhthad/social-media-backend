package media.social.modults.others.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.others.entity.Post;
import media.social.modults.others.exception.post.PostNotFoundException;
import media.social.modults.others.repository.PostRepository;
import media.social.modults.others.service.PostServiceDomain;
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
