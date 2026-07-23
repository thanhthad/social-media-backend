package media.social.modults.post.service;


import media.social.modults.post.dto.request.post.CreatePostRequest;
import media.social.modults.post.dto.request.post.UpdatePostContent;
import media.social.modults.post.dto.request.post.UpdatePostMedia;
import media.social.modults.post.dto.response.post.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PostService {

    Page<PostResponse> getFeed(Pageable pageable);

    void createPost(CreatePostRequest request);

    Page<PostResponse> getAllPostMe(Pageable pageable);

    Page<PostResponse> getAllPostByUserId(Long userId, Pageable pageable);

    PostResponse getPostById(Long postId);

    void deletePostMedia(Long PostMediaId);

    void updatePostContent(Long postId,UpdatePostContent content);

    void updatePostMedia(Long postId, UpdatePostMedia media);

    void deleteByPostId(Long id);

    Page<PostResponse> searchByContent(
            String keyword,
            Pageable pageable
    );

    Page<PostResponse> searchByHashtag(
            String hashtag,
            Pageable pageable
    );

    Page<PostResponse> getAllSavedPost(Pageable pageable);

}