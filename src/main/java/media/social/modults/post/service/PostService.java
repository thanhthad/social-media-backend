package media.social.modults.post.service;


import media.social.modults.post.dto.request.CreatePostRequest;
import media.social.modults.post.dto.request.UpdatePostRequest;
import media.social.modults.post.dto.response.PostResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PostService {

    PostResponse createPost(CreatePostRequest request);

    public Page<PostResponse> getAllPost(Long userId, Pageable pageable);


    PostResponse updatePost(Long id, UpdatePostRequest request);

    void deleteByPostId(Long id);

    PostResponse getByPostId(Long id);

    Page<PostResponse> getAllPosts(Pageable pageable);

    Page<PostResponse> getAllPostsByUserId(Long userId, Pageable pageable);

    void deleteByUserId ( Long id );

    Page<PostResponse> getByContent(String keyword , Pageable pageable);

    Page<PostResponse> getByCreatedAtBetween(LocalDateTime start, LocalDateTime end,Pageable pageable);

}