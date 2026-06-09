package media.social.modults.post.service.impl;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.post.dto.request.LikeRequest;
import media.social.modults.post.dto.response.LikeResponse;
import media.social.modults.post.entity.Like;
import media.social.modults.post.entity.Post;
import media.social.modults.user.dto.response.common.UserResponse;
import media.social.modults.user.entity.User;
import media.social.modults.post.exception.like.LikeAlreadyExistsException;
import media.social.modults.post.exception.like.LikeNotFoundException;
import media.social.modults.post.mapper.LikeMapper;
import media.social.modults.post.repository.LikeRepository;
import media.social.modults.user.security.userdetails.CustomUserDetails;
import media.social.modults.post.service.LikeService;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.service.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Log4j2
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final UserServiceDomain userServiceDomain;
    private final PostServiceDomain postServiceDomain;
    private final LikeMapper likeMapper;

    private Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()
                || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }

        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    // =========================================================
    // 1. LIKE POST
    // =========================================================
    @Override
    @Transactional
    public LikeResponse likePost(LikeRequest request) {

        Long actorUserId = getUserId();
        Long targetUserId = request.getUserId();
        Long postId = request.getPostId();

        log.info("LIKE_EVENT | action=LIKE_POST | actorUserId={} | targetUserId={} | postId={} | status=START",
                actorUserId, targetUserId, postId);

        if (likeRepository.existsByUserIdAndPostId(targetUserId, postId)) {

            log.warn("LIKE_EVENT | action=LIKE_POST | actorUserId={} | targetUserId={} | postId={} | status=FAIL | reason=ALREADY_EXISTS",
                    actorUserId, targetUserId, postId);

            throw new LikeAlreadyExistsException(
                    "Like already exists with userid: " + targetUserId + " and postid: " + postId
            );
        }

        User user = userServiceDomain.getByUserId(targetUserId);
        Post post = postServiceDomain.getByPostId(postId);

        Like like = likeMapper.toEntity(request, user, post);

        Like saved = likeRepository.save(like);

        log.info("LIKE_EVENT | action=LIKE_POST | actorUserId={} | targetUserId={} | postId={} | status=SUCCESS | likeId={}",
                actorUserId, targetUserId, postId, saved.getId());

        return likeMapper.toResponse(saved);
    }

    // =========================================================
    // 2. UNLIKE POST
    // =========================================================
    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {

        Long actorUserId = getUserId();

        log.info("LIKE_EVENT | action=UNLIKE_POST | actorUserId={} | targetUserId={} | postId={} | status=START",
                actorUserId, userId, postId);

        Like like = likeRepository.findByUserIdAndPostId(userId, postId)
                .orElseThrow(() -> {

                    log.warn("LIKE_EVENT | action=UNLIKE_POST | actorUserId={} | targetUserId={} | postId={} | status=FAIL | reason=NOT_FOUND",
                            actorUserId, userId, postId);

                    return new LikeNotFoundException("Like not found");
                });

        likeRepository.delete(like);

        log.info("LIKE_EVENT | action=UNLIKE_POST | actorUserId={} | targetUserId={} | postId={} | status=SUCCESS | likeId={}",
                actorUserId, userId, postId, like.getId());
    }

    // =========================================================
    // 3. COUNT LIKES
    // =========================================================
    @Override
    public long countLikesByPost(Long postId) {

        Long actorUserId = getUserId();

        log.info("LIKE_EVENT | action=COUNT_LIKES | actorUserId={} | postId={} | status=START",
                actorUserId, postId);

        postServiceDomain.validatePostExists(postId);

        long count = likeRepository.countByPostId(postId);

        log.info("LIKE_EVENT | action=COUNT_LIKES | actorUserId={} | postId={} | status=SUCCESS | count={}",
                actorUserId, postId, count);

        return count;
    }

    // =========================================================
    // 4. CHECK LIKE
    // =========================================================
    @Override
    public boolean isPostLiked(Long userId, Long postId) {

        Long actorUserId = getUserId();

        boolean result = likeRepository.existsByUserIdAndPostId(userId, postId);

        log.info("LIKE_EVENT | action=CHECK_LIKE | actorUserId={} | targetUserId={} | postId={} | liked={}",
                actorUserId, userId, postId, result);

        return result;
    }

    // =========================================================
    // 5. USERS WHO LIKED POST
    // =========================================================
    @Override
    public Page<UserResponse> getUsersWhoLikedPost(Long postId, Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("LIKE_EVENT | action=GET_LIKERS | actorUserId={} | postId={} | page={} | size={}",
                actorUserId, postId, pageable.getPageNumber(), pageable.getPageSize());

        postServiceDomain.validatePostExists(postId);

        Page<UserResponse> result = likeRepository.findUsersWhoLikedPost(postId, pageable)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .avatarUrl(user.getProfile().getAvatarUrl())
                        .build());

        log.info("LIKE_EVENT | action=GET_LIKERS | actorUserId={} | postId={} | status=SUCCESS | total={}",
                actorUserId, postId, result.getTotalElements());

        return result;
    }

//    // =========================================================
//    // 6. POSTS LIKED BY USER
//    // =========================================================
//    @Override
//    public Page<PostResponse> getPostsLikedByUser(Long userId, Pageable pageable) {
//
//        Long actorUserId = getUserId();
//
//        log.info("LIKE_EVENT | action=GET_USER_LIKES | actorUserId={} | targetUserId={} | page={} | size={}",
//                actorUserId, userId, pageable.getPageNumber(), pageable.getPageSize());
//
//        userServiceDomain.validateUserExists(userId);
//
//        Page<PostResponse> result = likeRepository.findByUserId(userId, pageable)
//                .map(like -> {
//
//                    Post post = like.getPost();
//                    User user = post.getUser();
//
//                    return PostResponse.builder()
//                            .id(post.getId())
//                            .content(post.getContent())
//                            .imageUrl(post.getImageUrl())
//                            .createdAt(post.getCreatedAt())
//                            .userId(user.getId())
//                            .username(user.getUsername())
//                            .avatarUrl(user.getProfile().getAvatarUrl())
//                            .build();
//                });
//
//        log.info("LIKE_EVENT | action=GET_USER_LIKES | actorUserId={} | targetUserId={} | status=SUCCESS | total={}",
//                actorUserId, userId, result.getTotalElements());
//
//        return result;
//    }

    // =========================================================
    // 7. TOP LIKED POSTS
    // =========================================================
    @Override
    public Page<Object[]> getTopLikedPosts(Pageable pageable) {

        Long actorUserId = getUserId();

        log.info("LIKE_EVENT | action=TOP_LIKED_POSTS | actorUserId={} | page={} | size={}",
                actorUserId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Object[]> result = likeRepository.findTopLikedPosts(pageable);

        log.info("LIKE_EVENT | action=TOP_LIKED_POSTS | actorUserId={} | status=SUCCESS | total={}",
                actorUserId, result.getTotalElements());

        return result;
    }
}