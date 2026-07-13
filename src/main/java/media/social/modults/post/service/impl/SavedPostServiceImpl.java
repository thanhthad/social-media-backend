package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.SavedPost;
import media.social.modults.post.entity.SavedPostId;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.exception.saved_post.SavedPostAlreadyExistsException;
import media.social.modults.post.exception.saved_post.SavedPostNotFoundException;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.post.repository.SavedPostRepository;
import media.social.modults.post.service.SavedPostService;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class SavedPostServiceImpl implements SavedPostService {

    private final SavedPostRepository savedPostRepository;
    private final UserServiceDomain userServiceDomain;
    private final PostRepository postRepository;

    @Override
    @Transactional
    public void savePost(Long postId) {
        Long userId = UserContextHolder.getUserId();

        User user = userServiceDomain.getByUserId(userId);

        Post post = postRepository.findById(postId).orElseThrow(() -> new PostNotFoundException("Post Not Found"));

        if(savedPostRepository.existsByUser_IdAndPost_Id(userId,postId)){
            throw new SavedPostAlreadyExistsException("you already saved this post");
        }
        SavedPost savedPost = SavedPost.builder()
                .id(new SavedPostId(userId,postId))
                .user(user)
                .post(post)
                .build();
        savedPostRepository.save(savedPost);
    }

    @Override
    @Transactional
    public void unsavePost(Long postId) {
        Long userId = UserContextHolder.getUserId();

        if(!postRepository.existsById(postId)){
            throw new PostNotFoundException("Post not found");
        }

        if(!savedPostRepository.existsByUser_IdAndPost_Id(userId,postId)){
            throw new SavedPostNotFoundException("you didn't save this post");
        }

        savedPostRepository.deleteByUser_IdAndPost_Id(userId,postId);
    }

    @Override
    public boolean isSaved(Long postId) {
        Long userId = UserContextHolder.getUserId();
        return savedPostRepository.existsByUser_IdAndPost_Id(userId,postId);
    }
}
