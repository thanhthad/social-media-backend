package media.social.modults.post.service;


import media.social.modults.post.dto.request.post.CreatePostRequest;
import media.social.modults.post.dto.request.post.UpdatePostContent;
import media.social.modults.post.dto.request.post.UpdatePostMedia;
import media.social.modults.post.dto.response.post.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface PostService {

    public void createPost(CreatePostRequest request);

    public Page<PostResponse> getAllPostMe(Pageable pageable);

    public Page<PostResponse> getAllPostByUserId(Long userId, Pageable pageable);

    public void deletePostMedia(String publicId);

    public void updatePostContent(Long postId,UpdatePostContent content);

    public void updatePostMedia(Long postId, UpdatePostMedia media);

    void deleteByPostId(Long id);

}