package media.social.modults.post.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.post.Enum.MediaType;
import media.social.modults.post.dto.request.CreatePostRequest;
import media.social.modults.post.dto.request.UpdatePostRequest;
import media.social.modults.post.dto.response.PostFlatResponse;
import media.social.modults.post.dto.response.PostResponse;
import media.social.modults.file.image.dto.response.UploadImageResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import media.social.modults.post.repository.PostMediaRepository;
import media.social.modults.user.entity.User;
import media.social.modults.post.exception.post.InvalidDateRangeException;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.mapper.PostMapper;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.post.service.PostService;
import media.social.modults.user.service.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
@AllArgsConstructor
@Transactional
@Log4j2
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final UserServiceDomain userServiceDomain;
    private final PostMapper postMapper;
    private final CloudinaryService cloudinaryService;


    // =========================================================
    // 1. CREATE POST
    // =========================================================
    @Override
    public PostResponse createPost(CreatePostRequest request) {

        Long UserId = UserContextHolder.getUserId();

        User user = userServiceDomain.getByUserId(UserId);
        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .build();

        Post savedPost = postRepository.save(post);

        for(int i = 0 ; i < request.getFiles().size() ; i++){
            cloudinaryService.validateImage(request.getFiles().get(i));
            UploadImageResponse uploadImageResponse = cloudinaryService.uploadImage(request.getFiles().get(i),"posts");
            PostMedia postMedia = PostMedia.builder()
                    .post(savedPost)
                    .mediaType(MediaType.IMAGE)
                    .url(uploadImageResponse.getImageUrl())
                    .publicId(uploadImageResponse.getPublicId())
                    .build();
            postMediaRepository.save(postMedia);
        }

        log.info("POST_EVENT | action=CREATE_POST | UserId={} |  postId={} | status=SUCCESS",
                UserId,  savedPost.getId());

        return postMapper.toResponse(savedPost);
    }

    public Page<PostResponse> getAllPost(Long userId, Pageable pageable) {

        Page<PostFlatResponse> flatPage =
                postRepository.findAllPostMe(userId, pageable);

        Map<Long, PostResponse> map = new LinkedHashMap<>();

        for (PostFlatResponse row : flatPage.getContent()) {

            PostResponse post = map.computeIfAbsent(row.getId(), id ->
                    PostResponse.builder()
                            .id(row.getId())
                            .content(row.getContent())
                            .createdAt(row.getCreatedAt())
                            .userId(row.getUserId())
                            .username(row.getUsername())
                            .avatarUrl(row.getAvatarUrl())
                            .imageUrl(new ArrayList<>())
                            .build()
            );

            if (row.getImageUrl() != null) {
                post.getImageUrl().add(row.getImageUrl());
            }
        }

        List<PostResponse> result = new ArrayList<>(map.values());

        return new PageImpl<>(
                result,
                pageable,
                flatPage.getTotalElements()
        );
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

    @Override
    public PostResponse getByPostId(Long id) {
        return null;
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