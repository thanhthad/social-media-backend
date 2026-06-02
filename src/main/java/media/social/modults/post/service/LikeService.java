package media.social.modults.post.service;

import media.social.modults.post.dto.request.LikeRequest;
import media.social.modults.post.dto.response.LikeResponse;
import media.social.modults.post.dto.response.PostResponse;
import media.social.modults.user.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeService {

    LikeResponse likePost(LikeRequest request);

    void unlikePost(Long userId, Long postId);

    long countLikesByPost(Long postId);

    boolean isPostLiked(Long userId, Long postId);

    Page<UserResponse> getUsersWhoLikedPost(Long postId, Pageable pageable);

//    Page<PostResponse> getPostsLikedByUser(Long userId, Pageable pageable);

    Page<Object[]> getTopLikedPosts(Pageable pageable);
}