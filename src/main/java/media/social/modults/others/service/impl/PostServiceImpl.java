package media.social.modults.others.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.others.dto.request.CreatePostRequest;
import media.social.modults.others.dto.request.UpdatePostRequest;
import media.social.modults.others.dto.response.PostResponse;
import media.social.modults.others.dto.response.UploadImageResponse;
import media.social.modults.others.entity.Post;
import media.social.modults.user.entity.User
import media.social.modults.others.exception.post.InvalidDateRangeException;
import media.social.modults.others.exception.post.PostNotFoundException;
import media.social.modults.others.mapper.PostMapper;
import media.social.modults.others.repository.PostRepository;
import media.social.modults.user.security.userdetails.CustomUserDetails;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.others.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@AllArgsConstructor
@Transactional
@Log4j2
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserServiceDomain userServiceDomain;
    private final PostMapper postMapper;
    private final CloudinaryService cloudinaryService;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    // =========================================================
    // 1. CREATE POST
    // =========================================================
    @Override
    public PostResponse createPost(CreatePostRequest request) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=CREATE_POST | actorUserId={} | targetUserId={} | status=START",
                actorUserId, request.getUserId());

        User user = userServiceDomain.getByUserId(request.getUserId());
        UploadImageResponse response = cloudinaryService.uploadImage(request.getFile(), "posts");
        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .imageUrl(response.getImageUrl())
                .imagePublicId(response.getPublicId())
                .build();

        Post savedPost = postRepository.save(post);

        log.info("POST_EVENT | action=CREATE_POST | actorUserId={} | targetUserId={} | postId={} | status=SUCCESS",
                actorUserId, request.getUserId(), savedPost.getId());

        return postMapper.toResponse(savedPost);
    }

    // =========================================================
    // 2. UPDATE POST
    // =========================================================
    @Override
    public PostResponse updatePost(Long id, UpdatePostRequest request) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=UPDATE_POST | actorUserId={} | postId={} | status=START",
                actorUserId, id);

        Post post = postRepository.findById(id).orElseThrow(() -> {
            log.warn("POST_EVENT | action=UPDATE_POST | actorUserId={} | postId={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, id);
            return new PostNotFoundException("Post not found with id: " + id);
        });

        UploadImageResponse uploadResponse = null;
        String newPublicId = null;
        String newImageUrl = null;

        if (request.getFile() != null && !request.getFile().isEmpty()) {
            uploadResponse = cloudinaryService.uploadImage(request.getFile(), "posts");
            newPublicId = uploadResponse.getPublicId();
            newImageUrl = uploadResponse.getImageUrl();
        }

        post.setContent(request.getContent());

        if (uploadResponse != null) {

            String oldPublicId = post.getImagePublicId();

            post.setImageUrl(newImageUrl);
            post.setImagePublicId(newPublicId);

            // delete old image AFTER update success data in memory
            if (oldPublicId != null && !oldPublicId.isBlank()) {
                try {
                    cloudinaryService.deleteImage(oldPublicId);
                } catch (Exception e) {
                    log.warn("POST_EVENT | action=DELETE_OLD_IMAGE_FAILED | publicId={}", oldPublicId, e);
                }
            }
        }

        Post saved = postRepository.save(post);

        log.info("POST_EVENT | action=UPDATE_POST | actorUserId={} | postId={} | status=SUCCESS",
                actorUserId, id);

        return postMapper.toResponse(saved);
    }

    // =========================================================
    // 3. DELETE POST
    // =========================================================
    @Override
    public void deleteByPostId(Long id) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=DELETE_POST | actorUserId={} | postId={} | status=START",
                actorUserId, id);

        Post post = postRepository.findById(id).orElseThrow(() -> {

            log.warn("POST_EVENT | action=DELETE_POST | actorUserId={} | postId={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, id);

            return new PostNotFoundException("Post not found with id: " + id);
        });
        cloudinaryService.deleteImage(post.getImagePublicId());
        postRepository.delete(post);

        log.info("POST_EVENT | action=DELETE_POST | actorUserId={} | postId={} | status=SUCCESS",
                actorUserId, id);
    }

    // =========================================================
    // 4. GET POST BY ID
    // =========================================================
    @Override
    public PostResponse getPostById(Long id) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=GET_POST | actorUserId={} | postId={} | status=START",
                actorUserId, id);

        Post post = postRepository.findById(id).orElseThrow(() -> {

            log.warn("POST_EVENT | action=GET_POST | actorUserId={} | postId={} | status=FAIL | reason=NOT_FOUND",
                    actorUserId, id);

            return new PostNotFoundException("Post not found with id: " + id);
        });

        return postMapper.toResponse(post);
    }

    // =========================================================
    // 5. GET ALL POSTS
    // =========================================================
    @Override
    public Page<PostResponse> getAllPosts(Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=GET_ALL_POSTS | actorUserId={} | page={} | size={}",
                actorUserId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Post> postPage = postRepository.findAll(pageable);

        log.info("POST_EVENT | action=GET_ALL_POSTS | actorUserId={} | status=SUCCESS | total={}",
                actorUserId, postPage.getTotalElements());

        return postPage.map(postMapper::toResponse);
    }

    // =========================================================
    // 6. GET POSTS BY USER ID
    // =========================================================
    @Override
    public Page<PostResponse> getAllPostsByUserId(Long userId, Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=GET_POSTS_BY_USER | actorUserId={} | targetUserId={} | page={} | size={}",
                actorUserId, userId, pageable.getPageNumber(), pageable.getPageSize());

        userServiceDomain.validateUserExists(userId);

        Page<Post> postPage = postRepository.findByUserId(userId, pageable);

        log.info("POST_EVENT | action=GET_POSTS_BY_USER | actorUserId={} | targetUserId={} | status=SUCCESS | total={}",
                actorUserId, userId, postPage.getTotalElements());

        return postPage.map(postMapper::toResponse);
    }

    // =========================================================
    // 7. DELETE BY USER ID
    // =========================================================
    @Override
    public void deleteByUserId(Long id) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=DELETE_BY_USER | actorUserId={} | targetUserId={} | status=START",
                actorUserId, id);

        userServiceDomain.validateUserExists(id);

        postRepository.deleteByUserId(id);

        log.info("POST_EVENT | action=DELETE_BY_USER | actorUserId={} | targetUserId={} | status=SUCCESS",
                actorUserId, id);
    }

    // =========================================================
    // 8. SEARCH BY CONTENT
    // =========================================================
    @Override
    public Page<PostResponse> getByContent(String keyword, Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=SEARCH_POST | actorUserId={} | keyword={} | page={} | size={}",
                actorUserId, keyword, pageable.getPageNumber(), pageable.getPageSize());

        Page<Post> postPage = postRepository.findByContentContainingIgnoreCase(keyword, pageable);

        log.info("POST_EVENT | action=SEARCH_POST | actorUserId={} | status=SUCCESS | total={}",
                actorUserId, postPage.getTotalElements());

        return postPage.map(postMapper::toResponse);
    }

    // =========================================================
    // 9. FILTER BY DATE RANGE
    // =========================================================
    @Override
    public Page<PostResponse> getByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {

        Long actorUserId = getUserId();

        log.info("POST_EVENT | action=FILTER_BY_DATE | actorUserId={} | start={} | end={}",
                actorUserId, start, end);

        if (start.isAfter(end)) {

            log.warn("POST_EVENT | action=FILTER_BY_DATE | actorUserId={} | status=FAIL | reason=INVALID_DATE_RANGE",
                    actorUserId);

            throw new InvalidDateRangeException("Start date must be before end date");
        }

        Page<Post> posts = postRepository.findByCreatedAtBetween(start, end, pageable);

        log.info("POST_EVENT | action=FILTER_BY_DATE | actorUserId={} | status=SUCCESS | total={}",
                actorUserId, posts.getTotalElements());

        return posts.map(postMapper::toResponse);
    }
}