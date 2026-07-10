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

    void deletePostMedia(String publicId);

    void updatePostContent(Long postId,UpdatePostContent content);

    void updatePostMedia(Long postId, UpdatePostMedia media);

    void deleteByPostId(Long id);



}