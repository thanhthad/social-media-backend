package media.social.modults.post.service.impl;


import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.post.dto.request.LikeRequest;
import media.social.modults.post.entity.Like;
import media.social.modults.post.entity.Post;
import media.social.modults.user.dto.response.user.UserResponse;
import media.social.modults.user.entity.User;
import media.social.modults.post.exception.like.LikeAlreadyExistsException;
import media.social.modults.post.exception.like.LikeNotFoundException;
import media.social.modults.post.mapper.LikeMapper;
import media.social.modults.post.repository.LikeRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.post.service.LikeService;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Log4j2
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final UserServiceDomain userServiceDomain;
    private final PostServiceDomain postServiceDomain;
    private final LikeMapper likeMapper;

    @Override
    @Transactional
    public void likePost(LikeRequest request) {

        Long userId = UserContextHolder.getUserId();
        Long postId = request.getPostId();

        if (likeRepository.existsByUserIdAndPostId(userId, postId)) {

            throw new LikeAlreadyExistsException(
                    "Like already exists with userid: " + userId + " and postid: " + postId
            );
        }

        User user = userServiceDomain.getByUserId(userId);
        Post post = postServiceDomain.getByPostId(postId);

        Like like = Like.builder()
                .user(user)
                .post(post)
                .build();

        Like saved = likeRepository.save(like);

    }

    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {

        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> new LikeNotFoundException("Like not found"));

        likeRepository.delete(like);

    }

    @Override
    @Transactional(readOnly = true)
    public long countLikesByPost(Long postId) {

        postServiceDomain.validatePostExists(postId);

        return likeRepository.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isPostLiked(Long postId) {
        Long userId = UserContextHolder.getUserId();

        return likeRepository.existsByUserIdAndPostId(userId,postId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersWhoLikedPost(Long postId, Pageable pageable) {

        postServiceDomain.validatePostExists(postId);

        return likeRepository.findUsersLikedPost(postId, pageable);
    }

}