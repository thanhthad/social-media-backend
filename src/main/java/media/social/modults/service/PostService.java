package media.social.modults.service;


import media.social.modults.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostService {

    Post createPost(Long userId, Post post);

    Post updatePost(Long id, Post post);

    void deletePost(Long id);

    Optional<Post> getPostById(Long id);

    List<Post> getAllPosts();

    List<Post> getPostsByUser(Long userId);
}