package media.social.modults.post.service;

import media.social.modults.post.dto.request.LikeRequest;
import media.social.modults.user.dto.response.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeService {

    void likePost(LikeRequest request);

    void unlikePost(Long userId, Long postId);

    long countLikesByPost(Long postId);

    boolean isPostLiked( Long postId);

    Page<UserResponse> getUsersWhoLikedPost(Long postId, Pageable pageable);

}