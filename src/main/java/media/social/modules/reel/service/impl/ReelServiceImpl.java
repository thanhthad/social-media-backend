package media.social.modules.reel.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.PostMedia;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.repository.PostMediaRepository;
import media.social.modules.post.repository.PostRepository;
import media.social.modules.post.repository.ReactionRepository;
import media.social.modules.reel.dto.projection.ReelFlatProjection;
import media.social.modules.reel.dto.request.CreateReelRequest;
import media.social.modules.reel.dto.request.UpdateReelContentRequest;
import media.social.modules.reel.dto.response.ReelResponse;
import media.social.modules.reel.entity.ReelDetail;
import media.social.modules.reel.exception.ReelForbiddenException;
import media.social.modules.reel.exception.ReelNotFoundException;
import media.social.modules.reel.repository.ReelRepository;
import media.social.modules.reel.service.ReelService;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.user.service.domain.BlockPolicyService;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@AllArgsConstructor
@Log4j2
public class ReelServiceImpl implements ReelService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final ReelRepository reelRepository;
    private final UserServiceDomain userServiceDomain;
    private final MediaUploadService mediaUploadService;
    private final BlockPolicyService blockPolicyService;
    private final FriendShipDomain friendShipDomain;
    private final ReactionRepository reactionRepository;

    // ─── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void createReel(CreateReelRequest request) {

        Long userId = UserContextHolder.getUserId();
        User user = userServiceDomain.getByUserId(userId);

        // 1. Upload video
        UploadFileResponse videoUpload =
                mediaUploadService.upload(request.getVideo(), MediaUploadContext.REEL);

        // 2. Media processing: duration, width, height from video upload response
        int durationSeconds = (int) videoUpload.getDuration();
        int width = videoUpload.getWidth() != null ? videoUpload.getWidth() : 0;
        int height = videoUpload.getHeight() != null ? videoUpload.getHeight() : 0;

        // 3. Handle thumbnail (custom or auto-generated ~1s frame from video)
        String thumbnailUrl;
        String thumbnailPublicId = null;

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            UploadFileResponse thumbnailUpload =
                    mediaUploadService.upload(request.getThumbnail(), MediaUploadContext.REEL);
            thumbnailUrl = thumbnailUpload.getFileUrl();
            thumbnailPublicId = thumbnailUpload.getPublicId();
        } else {
            thumbnailUrl = mediaUploadService.generateVideoThumbnailUrl(videoUpload.getPublicId());
        }

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .postType(PostType.REEL)
                .build();

        postRepository.save(post);

        PostMedia videoMedia = PostMedia.builder()
                .post(post)
                .mediaType(MediaType.VIDEO)
                .url(videoUpload.getFileUrl())
                .publicId(videoUpload.getPublicId())
                .build();

        postMediaRepository.save(videoMedia);

        ReelDetail reelDetail = ReelDetail.builder()
                .post(post)
                .durationSeconds(durationSeconds)
                .width(width)
                .height(height)
                .thumbnailUrl(thumbnailUrl)
                .thumbnailPublicId(thumbnailPublicId)
                .build();

        reelRepository.save(reelDetail);
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReelResponse getReelById(Long reelId) {

        Long viewerId = UserContextHolder.getUserId();

        // Load post để check owner + block
        Post post = postRepository.findByIdWithUser(reelId, PostType.REEL)
                .orElseThrow(() -> new ReelNotFoundException("Reel not found: " + reelId));

        Long ownerId = post.getUser().getId();

        if (blockPolicyService.isBlocked(viewerId, ownerId)) {
            throw new UserBlockedException("You cannot view this reel.");
        }

        List<Visibility> visibilities;

        if (viewerId.equals(ownerId)) {
            visibilities = List.of(Visibility.PUBLIC, Visibility.FRIEND, Visibility.PRIVATE);
        } else if (friendShipDomain.areFriends(viewerId, ownerId)) {
            visibilities = List.of(Visibility.PUBLIC, Visibility.FRIEND);
        } else {
            visibilities = List.of(Visibility.PUBLIC);
        }

        ReelFlatProjection flat = reelRepository.findReelDetailById(
                reelId,
                PostType.REEL,
                Status.ACTIVE,
                visibilities,
                ReportStatus.APPROVED
        ).orElseThrow(() -> new ReelNotFoundException("Reel not found or not accessible: " + reelId));

        // Video URL từ post_media
        String videoUrl = postMediaRepository.findByPostId(reelId)
                .stream()
                .filter(m -> m.getMediaType() == MediaType.VIDEO)
                .map(PostMedia::getUrl)
                .findFirst()
                .orElse(null);

        var reaction = reactionRepository.findByUserIdAndPostId(viewerId, reelId).orElse(null);

        return toResponse(flat, videoUrl, reaction != null, reaction != null ? reaction.getType() : null);
    }

    // ─── FEED ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReelResponse> getReelFeed(Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        Page<ReelFlatProjection> flatPage = reelRepository.findReelFeed(
                viewerId,
                Status.ACTIVE,
                PostType.REEL,
                ReportStatus.APPROVED,
                FriendshipStatus.ACCEPTED,
                Visibility.PUBLIC,
                Visibility.FRIEND,
                pageable
        );

        return buildPage(flatPage, viewerId, pageable);
    }

    // ─── EXPLORE ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReelResponse> getReelExplore(Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        Page<ReelFlatProjection> flatPage = reelRepository.findReelExplore(
                viewerId,
                Status.ACTIVE,
                PostType.REEL,
                ReportStatus.APPROVED,
                Visibility.PUBLIC,
                FriendshipStatus.ACCEPTED,
                pageable
        );

        return buildPage(flatPage, viewerId, pageable);
    }

    // ─── MY REELS ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReelResponse> getMyReels(Pageable pageable) {

        Long userId = UserContextHolder.getUserId();

        Page<ReelFlatProjection> flatPage = reelRepository.findAllReelMe(
                userId,
                Status.ACTIVE,
                ReportStatus.APPROVED,
                PostType.REEL,
                pageable
        );

        return buildPage(flatPage, userId, pageable);
    }

    // ─── USER REELS ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReelResponse> getUserReels(Long targetUserId, Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        if (blockPolicyService.isBlocked(viewerId, targetUserId)) {
            throw new UserBlockedException("You cannot view this user's reels.");
        }

        Page<ReelFlatProjection> flatPage = reelRepository.findAllVisibleReelsByUser(
                viewerId,
                targetUserId,
                Status.ACTIVE,
                ReportStatus.APPROVED,
                PostType.REEL,
                Visibility.PUBLIC,
                Visibility.FRIEND,
                FriendshipStatus.ACCEPTED,
                pageable
        );

        return buildPage(flatPage, viewerId, pageable);
    }

    // ─── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void updateReel(Long reelId, UpdateReelContentRequest request) {

        Post post = postRepository.findByIdWithUser(reelId, PostType.REEL)
                .orElseThrow(() -> new ReelNotFoundException("Reel not found: " + reelId));

        checkOwner(post);

        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }

        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }
    }

    // ─── DELETE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReel(Long reelId) {

        Post post = postRepository.findByIdWithUser(reelId, PostType.REEL)
                .orElseThrow(() -> new ReelNotFoundException("Reel not found: " + reelId));

        checkOwner(post);

        reelRepository.findByPostId(reelId).ifPresent(detail -> {
            if (detail.getThumbnailPublicId() != null && !detail.getThumbnailPublicId().isBlank()) {
                mediaUploadService.delete(
                        detail.getThumbnailPublicId(),
                        MediaType.IMAGE
                );
            }
            reelRepository.delete(detail);
        });

        postMediaRepository.findByPostId(reelId)
                .forEach(m -> mediaUploadService.delete(
                        m.getPublicId(),
                        m.getMediaType()
                ));

        postMediaRepository.deleteByPostId(reelId);

        postRepository.delete(post);
    }

    // ─── COUNTERS ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void incrementViewCount(Long reelId) {
        if (reelRepository.incrementViewCount(reelId) == 0) {
            throw new ReelNotFoundException("Reel not found: " + reelId);
        }
    }

    @Override
    @Transactional
    public void incrementShareCount(Long reelId) {
        if (reelRepository.incrementShareCount(reelId) == 0) {
            throw new ReelNotFoundException("Reel not found: " + reelId);
        }
    }

    // ─── PRIVATE HELPERS ───────────────────────────────────────────────────────

    private void checkOwner(Post post) {
        Long currentUserId = UserContextHolder.getUserId();
        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ReelForbiddenException("You are not allowed to modify this reel.");
        }
    }

    /**
     * Chuyển Page<ReelFlatProjection> → Page<ReelResponse>.
     * Batch-load video URL + reaction của viewer.
     */
    private Page<ReelResponse> buildPage(
            Page<ReelFlatProjection> flatPage,
            Long viewerId,
            Pageable pageable
    ) {
        if (flatPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, flatPage.getTotalElements());
        }

        List<Long> postIds = flatPage.getContent()
                .stream()
                .map(ReelFlatProjection::getId)
                .toList();

        // Batch load video URLs
        var mediaList = postMediaRepository.findMediaByPostIds(postIds);

        var videoUrlMap = mediaList.stream()
                .filter(m -> m.getType() == media.social.modules.post.enums.MediaType.VIDEO)
                .collect(java.util.stream.Collectors.toMap(
                        m -> m.getPostId(),
                        m -> m.getUrl(),
                        (a, b) -> a  // giữ cái đầu nếu trùng
                ));

        // Batch load reactions
        var reactions = reactionRepository.findMyReactions(viewerId, postIds);

        var reactionMap = reactions.stream()
                .collect(java.util.stream.Collectors.toMap(
                        r -> r.getPost().getId(),
                        r -> r
                ));

        List<ReelResponse> result = flatPage.getContent()
                .stream()
                .map(row -> {
                    var reaction = reactionMap.get(row.getId());
                    return toResponse(
                            row,
                            videoUrlMap.get(row.getId()),
                            reaction != null,
                            reaction != null ? reaction.getType() : null
                    );
                })
                .toList();

        return new PageImpl<>(result, pageable, flatPage.getTotalElements());
    }

    private ReelResponse toResponse(
            ReelFlatProjection flat,
            String videoUrl,
            boolean reacted,
            ReactionType myReactionType
    ) {
        return ReelResponse.builder()
                .id(flat.getId())
                .content(flat.getContent())
                .visibility(flat.getVisibility())
                .createdAt(flat.getCreatedAt())
                .userId(flat.getUserId())
                .username(flat.getUsername())
                .avatarUrl(flat.getAvatarUrl())
                .commentCount(flat.getCommentCount())
                .reactionCount(flat.getReactionCount())
                .videoUrl(videoUrl)
                .thumbnailUrl(flat.getThumbnailUrl())
                .durationSeconds(flat.getDurationSeconds())
                .width(flat.getWidth())
                .height(flat.getHeight())
                .viewCount(flat.getViewCount())
                .shareCount(flat.getShareCount())
                .reacted(reacted)
                .myReactionType(myReactionType)
                .build();
    }
}
