package media.social.modults.service.impl;

import media.social.modults.entity.Post;
import media.social.modults.entity.User;
import media.social.modults.repository.PostRepository;
import media.social.modults.repository.UserRepository;
import media.social.modults.service.PostService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public PostServiceImpl(PostRepository postRepository,
                           UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    // CREATE POST (gắn user)
    @Override
    public Post createPost(Long userId, Post post) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        post.setUser(user);
        return postRepository.save(post);
    }

    // UPDATE POST
    @Override
    public Post updatePost(Long id, Post post) {
        Post existing = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        existing.setContent(post.getContent());
        existing.setImageUrl(post.getImageUrl());

        return postRepository.save(existing);
    }

    // DELETE
    @Override
    public void deletePost(Long id) {
        postRepository.deleteById(id);
    }

    // GET BY ID
    @Override
    public Optional<Post> getPostById(Long id) {
        return postRepository.findById(id);
    }

    // GET ALL
    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    // JOIN: posts by userId
    @Override
    public List<Post> getPostsByUser(Long userId) {
        return postRepository.findByUserId(userId);
    }
}