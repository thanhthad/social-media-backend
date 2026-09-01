package media.social.modules.post.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modules.auth.Enum.Status;
import media.social.modules.post.dto.projection.ListPostMediaProjection;
import media.social.modules.post.dto.projection.PostFlatProjection;
import media.social.modules.post.dto.request.post.UpdatePostVisibility;
import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.entity.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.dto.request.post.CreatePostRequest;
import media.social.modules.post.dto.request.post.UpdatePostContent;
import media.social.modules.post.dto.request.post.UpdatePostMedia;
import media.social.modules.post.dto.response.post.PostMediaResponse;
import media.social.modules.post.dto.response.post.PostResponse;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post_media.MediaNotFoundException;
import media.social.modules.post.repository.*;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.post.service.cache.PostCacheService;
import media.social.modules.user.entity.User;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.post.service.PostService;
import media.social.modules.user.service.domain.BlockPolicyService;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@AllArgsConstructor
@Log4j2
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final UserServiceDomain userServiceDomain;
    private final MediaUploadService mediaUploadService;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final BlockPolicyService blockPolicyService;
    private final FriendShipDomain friendShipDomain;
    private final PostDomainService postDomainService;
    private final ReactionRepository reactionRepository;
    private final CommentRepository commentRepository;
    private final PostCacheService postCacheService;

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        Page<PostFlatProjection> flatPage =
                postRepository.findFeed(
                        viewerId,
                        Status.ACTIVE,
                        PostType.POST,
                        ReportStatus.APPROVED,
                        FriendshipStatus.ACCEPTED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        pageable
                );

        return buildPostResponse(flatPage, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getExplore(Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        // findExplore dùng nativeQuery nên phải truyền String (.name()) thay vì Enum trực tiếp
        Page<PostFlatProjection> flatPage =
                postRepository.findExplore(
                        viewerId,
                        Status.ACTIVE.name(),
                        PostType.POST.name(),
                        ReportStatus.APPROVED.name(),
                        Visibility.PUBLIC.name(),
                        FriendshipStatus.ACCEPTED.name(),
                        pageable
                );

        return buildPostResponse(flatPage, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostMe(Pageable pageable) {
        Long userId = UserContextHolder.getUserId();

        Page<PostFlatProjection> flatPage =
                postRepository.findAllPostMe(
                        userId,
                        Status.ACTIVE,
                        ReportStatus.APPROVED,
                        PostType.POST,
                        pageable
                );

        return buildPostResponse(flatPage, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostByUserId(
            Long targetUserId,
            Pageable pageable
    ) {
        Long viewerId = UserContextHolder.getUserId();

        if (blockPolicyService.isBlocked(viewerId, targetUserId)) {
            throw new UserBlockedException(
                    "You cannot view this user's posts."
            );
        }

        Page<PostFlatProjection> flatPage =
                postRepository.findAllVisiblePost(
                        viewerId,
                        targetUserId,
                        Status.ACTIVE,
                        ReportStatus.APPROVED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        FriendshipStatus.ACCEPTED,
                        PostType.POST,
                        pageable
                );

        return buildPostResponse(
                flatPage,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId) {

        Long viewerId = UserContextHolder.getUserId();

        Post post = postRepository.findByIdWithUser(postId, PostType.POST)
                .orElseThrow(() ->
                        new PostNotFoundException("Post not found")
                );

        Long ownerId = post.getUser().getId();

        if (blockPolicyService.isBlocked(viewerId, ownerId)) {
            throw new UserBlockedException(
                    "You cannot view this user's posts."
            );
        }

        List<Visibility> visibilities;

        if (viewerId.equals(ownerId)) {

            visibilities = List.of(
                    Visibility.PUBLIC,
                    Visibility.FRIEND,
                    Visibility.PRIVATE
            );
        } else if (friendShipDomain.areFriends(viewerId, ownerId)) {

            visibilities = List.of(
                    Visibility.PUBLIC,
                    Visibility.FRIEND
            );
        } else {
            visibilities = List.of(
                    Visibility.PUBLIC
            );
        }

        PostCacheDTO flat =
                postCacheService.getPost(
                        postId,
                        visibilities
                );

        Reaction reaction =
                reactionRepository
                        .findByUserIdAndPostId(viewerId, postId)
                        .orElse(null);

        long realReactionCount = reactionRepository.countByPostId(postId);
        long realCommentCount = commentRepository.countByPostId(postId);

        return PostResponse.builder()
                .id(flat.getId())
                .content(flat.getContent())
                .visibility(flat.getVisibility())
                .createdAt(flat.getCreatedAt())
                .userId(flat.getUserId())
                .username(flat.getUsername())
                .avatarUrl(flat.getAvatarUrl())
                .postMediaResponses(flat.getPostMediaResponses())
                .commentCount(realCommentCount)
                .reactionCount(realReactionCount)
                .reacted(reaction != null)
                .myReactionType(
                        reaction != null
                                ? reaction.getType()
                                : null
                )
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> searchByContent(
            String keyword,
            Pageable pageable
    ) {
        Long viewerId = UserContextHolder.getUserId();

        Page<PostFlatProjection> projections =
                postRepository.searchByContent(
                        viewerId,
                        keyword.trim(),
                        Status.ACTIVE.name(),
                        PostType.POST.name(),
                        ReportStatus.APPROVED.name(),
                        Visibility.PUBLIC.name(),
                        Visibility.FRIEND.name(),
                        FriendshipStatus.ACCEPTED.name(),
                        pageable
                );

        return buildPostResponse(projections, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> searchByHashtag(
            String hashtag,
            Pageable pageable
    ) {
        Long viewerId = UserContextHolder.getUserId();

        Page<PostFlatProjection> page =
                postRepository.searchByHashtag(
                        viewerId,
                        hashtag.trim().toLowerCase(),
                        Status.ACTIVE,
                        PostType.POST,
                        ReportStatus.APPROVED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        FriendshipStatus.ACCEPTED,
                        pageable
                );

        return buildPostResponse(page, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllSavedPost(Pageable pageable) {

        Long userId = UserContextHolder.getUserId();

        Page<PostFlatProjection> postFlatResponses =
                postRepository.findSavedPosts(
                        userId,
                        Status.ACTIVE,
                        PostType.POST,
                        ReportStatus.APPROVED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        FriendshipStatus.ACCEPTED,
                        pageable
                );

        return buildPostResponse(
                postFlatResponses,
                pageable
        );
    }

    private Page<PostResponse> buildPostResponse(
            Page<PostFlatProjection> flatPage,
            Pageable pageable
    ) {

        if (flatPage.isEmpty()) {
            return new PageImpl<>(
                    Collections.emptyList(),
                    pageable,
                    flatPage.getTotalElements()
            );
        }

        List<Long> postIds = flatPage.getContent()
                .stream()
                .map(PostFlatProjection::getId)
                .toList();

        List<ListPostMediaProjection> mediaList =
                postMediaRepository.findMediaByPostIds(postIds);

        Map<Long, List<PostMediaResponse>> mediaMap = new HashMap<>();

        for (ListPostMediaProjection m : mediaList) {
            mediaMap
                    .computeIfAbsent(
                            m.getPostId(),
                            k -> new ArrayList<>()
                    )
                    .add(
                            PostMediaResponse.builder()
                                    .url(m.getUrl())
                                    .postMediaId(m.getPostMediaId())
                                    .type(m.getType())
                                    .build()
                    );
        }

        Long userId = UserContextHolder.getUserId();

        List<Reaction> reactions =
                reactionRepository.findMyReactions(
                        userId,
                        postIds
                );

        Map<Long, Reaction> reactionMap =
                reactions.stream()
                        .collect(Collectors.toMap(
                                r -> r.getPost().getId(),
                                r -> r
                        ));

        Map<Long, Long> reactionCountMap = new HashMap<>();
        for (Object[] rCount : reactionRepository.countReactionsByPostIds(postIds)) {
            if (rCount != null && rCount.length >= 2) {
                reactionCountMap.put((Long) rCount[0], (Long) rCount[1]);
            }
        }

        Map<Long, Long> commentCountMap = new HashMap<>();
        for (Object[] cCount : commentRepository.countCommentsByPostIds(postIds)) {
            if (cCount != null && cCount.length >= 2) {
                commentCountMap.put((Long) cCount[0], (Long) cCount[1]);
            }
        }

        List<PostResponse> result =
                flatPage.getContent()
                        .stream()
                        .map(row -> {
                            Reaction reaction =
                                    reactionMap.get(row.getId());
                            long rCount = reactionCountMap.getOrDefault(
                                    row.getId(),
                                    row.getReactionCount() != null ? row.getReactionCount() : 0L
                            );
                            long cCount = commentCountMap.getOrDefault(
                                    row.getId(),
                                    row.getCommentCount() != null ? row.getCommentCount() : 0L
                            );
                            return PostResponse.builder()
                                    .id(row.getId())
                                    .content(row.getContent())
                                    .visibility(row.getVisibility())
                                    .createdAt(row.getCreatedAt())

                                    .userId(row.getUserId())
                                    .username(row.getUsername())
                                    .avatarUrl(row.getAvatarUrl())
                                    .commentCount(cCount)
                                    .reactionCount(rCount)
                                    .reacted(reaction != null)
                                    .myReactionType(
                                            reaction != null
                                                    ? reaction.getType()
                                                    : null
                                    )
                                    .postMediaResponses(
                                            mediaMap.getOrDefault(
                                                    row.getId(),
                                                    Collections.emptyList()
                                             )
                                     )
                                     .build();

                        })
                        .toList();
        return new PageImpl<>(
                result,
                pageable,
                flatPage.getTotalElements()
        );
    }

    @Override
    @Transactional
    public void createPost(CreatePostRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userServiceDomain.getByUserId(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .build();

        postRepository.save(post);

        savePostMedia(post, request.getFiles());

        savePostHashtags(post, request.getContent());

    }

    @Override
    @Transactional
    public void updatePostContent(Long postId, UpdatePostContent request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new PostNotFoundException(
                                "Post not found with id: " + postId
                        )
                );

        postDomainService.checkOwner(post);

        long mediaCount = postMediaRepository.countByPostId(postId);

        if (request.getContent().isBlank() && mediaCount == 0) {
            throw new IllegalArgumentException(
                    "Post must contain content or media."
            );
        }

        post.setContent(request.getContent());

        postCacheService.evictPost(postId);
        updatePostHashtags(post, request.getContent());
    }

    @Override
    @Transactional
    public void updatePostVisibility(
            Long postId,
            UpdatePostVisibility request
    ) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new PostNotFoundException(
                                "Post not found with id: " + postId
                        )
                );

        postDomainService.checkOwner(post);

        postCacheService.evictPost(postId);
        post.setVisibility(request.getVisibility());
    }

    @Override
    @Transactional
    public void deleteByPostId(Long postId) {

        Long userId = UserContextHolder.getUserId();

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new PostNotFoundException(
                                "Post not found with id: " + postId
                        )
                );

        postDomainService.checkOwner(post);

        postMediaRepository.findByPostId(postId)
                .forEach(media ->
                        mediaUploadService.delete(
                                media.getPublicId(),
                                media.getMediaType()
                        ));

        postMediaRepository.deleteByPostId(postId);

        deleteUnusedHashtags(postId);

        postRepository.delete(post);
        postCacheService.evictPost(postId);
    }

    @Override
    @Transactional
    public void updatePostMedia(Long postId, UpdatePostMedia request) {

        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new IllegalArgumentException("No media uploaded.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() ->
                        new PostNotFoundException(
                                "Post not found with id: " + postId
                        )
                );

        postDomainService.checkOwner(post);

        postCacheService.evictPost(postId);
        savePostMedia(post, request.getFiles());
    }

    @Override
    @Transactional
    public void deletePostMedia(Long PostMediaId) {

        PostMedia media = postMediaRepository.findById(PostMediaId)
                .orElseThrow(() ->
                        new MediaNotFoundException("Media not found: " + PostMediaId)
                );

        Post post = media.getPost();

        postDomainService.checkOwner(post);

        long totalMedia = postMediaRepository.countByPostId(post.getId());

        boolean emptyContent =
                post.getContent() == null ||
                        post.getContent().isBlank();

        if (totalMedia == 1 && emptyContent) {
            throw new IllegalStateException(
                    "Post must contain at least one media or content."
            );
        }

        mediaUploadService.delete(media.getPublicId(), media.getMediaType());

        postCacheService.evictPost(post.getId());
        postMediaRepository.delete(media);
    }

    private void savePostHashtags(Post post, String content) {

        Set<String> hashtagNames = extractHashtags(content);

        if (hashtagNames.isEmpty()) {
            return;
        }

        List<PostHashtag> relations = new ArrayList<>();

        for (String name : hashtagNames) {

            name = name.trim().toLowerCase();

            String finalName = name;

            Hashtag hashtag = hashtagRepository.findByName(name)
                    .orElseGet(() ->
                            hashtagRepository.save(
                                    Hashtag.builder()
                                            .name(finalName)
                                            .build()
                            ));

            relations.add(
                    PostHashtag.builder()
                            .post(post)
                            .hashtag(hashtag)
                            .build()
            );
        }

        postHashtagRepository.saveAll(relations);
    }

    private void updatePostHashtags(Post post, String content) {

        deleteUnusedHashtags(post.getId());

        savePostHashtags(post, content);
    }

    private void deleteUnusedHashtags(Long postId) {

        List<PostHashtag> relations =
                postHashtagRepository.findByPost_Id(postId);

        for (PostHashtag relation : relations) {

            Hashtag hashtag = relation.getHashtag();

            postHashtagRepository.delete(relation);

            hashtagRepository.deleteIfUnused(hashtag.getHashtagId());
        }
    }

    private Set<String> extractHashtags(String content) {

        if (content == null || content.isBlank()) {
            return Collections.emptySet();
        }

        Pattern pattern =
                Pattern.compile("#([\\p{L}\\p{N}_]+)");

        Matcher matcher = pattern.matcher(content);

        Set<String> hashtags = new HashSet<>();

        while (matcher.find()) {
            hashtags.add(matcher.group(1));
        }

        return hashtags;
    }

    private void savePostMedia(Post post, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return;
        }

        List<PostMedia> mediaList = new ArrayList<>(files.size());

        for (MultipartFile file : files) {

            UploadFileResponse upload =
                    mediaUploadService.upload(file, MediaUploadContext.POST);

            mediaList.add(PostMedia.builder()
                    .post(post)
                    .mediaType(MediaType.valueOf(upload.getResourceType()))
                    .url(upload.getFileUrl())
                    .publicId(upload.getPublicId())
                    .build());
        }

        postMediaRepository.saveAll(mediaList);
    }

}