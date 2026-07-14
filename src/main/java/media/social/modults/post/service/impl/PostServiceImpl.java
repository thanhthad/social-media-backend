package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.post.entity.Hashtag;
import media.social.modults.post.entity.PostHashtag;
import media.social.modults.post.enums.MediaType;
import media.social.modults.post.dto.request.post.CreatePostRequest;
import media.social.modults.post.dto.request.post.UpdatePostContent;
import media.social.modults.post.dto.request.post.UpdatePostMedia;
import media.social.modults.post.dto.response.post.PostFlatResponse;
import media.social.modults.post.dto.response.post.PostMediaResponse;
import media.social.modults.post.dto.response.post.PostResponse;
import media.social.modults.file.image.dto.response.UploadFileResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import media.social.modults.post.enums.Visibility;
import media.social.modults.post.exception.post_media.MediaNotFoundException;
import media.social.modults.post.repository.HashtagRepository;
import media.social.modults.post.repository.PostHashtagRepository;
import media.social.modults.post.repository.PostMediaRepository;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.entity.User;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.user.exception.block.UserBlockedException;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.file.image.service.CloudinaryService;
import media.social.modults.post.service.PostService;
import media.social.modults.user.service.FollowService;
import media.social.modults.user.service.domain.BlockPolicyService;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
@AllArgsConstructor
@Log4j2
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final UserServiceDomain userServiceDomain;
    private final CloudinaryService cloudinaryService;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final BlockPolicyService blockPolicyService;
    private final FollowService followService;
    private final PostServiceDomain postServiceDomain;

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        Page<PostFlatResponse> flatPage =
                postRepository.findFeed(viewerId, pageable);

        return buildPostResponse(flatPage, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostMe(Pageable pageable) {
        Long userId = UserContextHolder.getUserId();
        return getAllPost(userId,pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostByUserId(Long targetUserId, Pageable pageable) {

        Long viewerId = UserContextHolder.getUserId();

        if (blockPolicyService.isBlocked(viewerId, targetUserId)) {
            throw new UserBlockedException("You cannot view this user's posts.");
        }

        if (viewerId.equals(targetUserId)) {
            return getAllPost(targetUserId, pageable);
        }

        List<Visibility> visibilities;

        if (followService.isFollowing(targetUserId)) {
            visibilities = List.of(
                    Visibility.PUBLIC,
                    Visibility.FOLLOWERS
            );
        } else {
            visibilities = List.of(
                    Visibility.PUBLIC
            );
        }

        return getAllPost(targetUserId, visibilities, pageable);
    }


    @Transactional(readOnly = true)
    private Page<PostResponse> getAllPost(Long userId, Pageable pageable) {

        Page<PostFlatResponse> flatPage =
                postRepository.findAllPostMe(userId, pageable);

        return buildPostResponse(flatPage,pageable);
    }
    @Transactional(readOnly = true)
    private Page<PostResponse> getAllPost(Long userId,List<Visibility> visibilities, Pageable pageable) {

        Page<PostFlatResponse> flatPage =
                postRepository.findAllVisiblePost(userId,visibilities, pageable);

        return buildPostResponse(flatPage,pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId) {
        Long viewerId = UserContextHolder.getUserId();

        Post post = postRepository.findByIdWithUser(postId)
                .orElseThrow(() ->
                        new PostNotFoundException("Post not found")
                );

        Long ownerId = post.getUser().getId();
        if (blockPolicyService.isBlocked(viewerId, ownerId)) {
            throw new UserBlockedException("You cannot view this user's posts.");
        }

        List<Visibility> visibilities;

        if (viewerId.equals(ownerId)) {
            visibilities = List.of(
                    Visibility.PUBLIC,
                    Visibility.FOLLOWERS,
                    Visibility.PRIVATE
            );
        } else if (followService.isFollowing(ownerId)) {
            visibilities = List.of(
                    Visibility.PUBLIC,
                    Visibility.FOLLOWERS
            );
        } else {
            visibilities = List.of(
                    Visibility.PUBLIC
            );
        }

        PostFlatResponse flat = postRepository.findPostDetailById(postId,visibilities)
                .orElseThrow(() ->
                        new PostNotFoundException("Post not found with id: " + postId)
                );

        return PostResponse.builder()
                .id(flat.getId())
                .content(flat.getContent())
                .visibility(flat.getVisibility())
                .createdAt(flat.getCreatedAt())
                .userId(flat.getUserId())
                .username(flat.getUsername())
                .avatarUrl(flat.getAvatarUrl())
                .postMediaResponses(
                        postMediaRepository.findMediaResponseByPostId(postId)
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

        Page<PostFlatResponse> page =
                postRepository.searchByContent(
                        viewerId,
                        keyword.trim(),
                        pageable
                );

        return buildPostResponse(page, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> searchByHashtag(
            String hashtag,
            Pageable pageable
    ) {

        Long viewerId = UserContextHolder.getUserId();

        Page<PostFlatResponse> page =
                postRepository.searchByHashtag(
                        viewerId,
                        hashtag.trim().toLowerCase(),
                        pageable
                );

        return buildPostResponse(page, pageable);
    }

    @Override
    public Page<PostResponse> getAllSavedPost(Pageable pageable) {
        Long userId = UserContextHolder.getUserId();

        Page<PostFlatResponse> postFlatResponses = postRepository.findSavedPosts(userId,pageable);

        return buildPostResponse(postFlatResponses,pageable);
    }

    private Page<PostResponse> buildPostResponse(
            Page<PostFlatResponse> flatPage,
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
                .map(PostFlatResponse::getId)
                .toList();

        List<PostMedia> mediaList =
                postMediaRepository.findByPostIdIn(postIds);

        Map<Long, List<PostMediaResponse>> mediaMap = new HashMap<>();

        for (PostMedia m : mediaList) {

            mediaMap
                    .computeIfAbsent(
                            m.getPost().getId(),
                            k -> new ArrayList<>()
                    )
                    .add(PostMediaResponse.builder()
                            .url(m.getUrl())
                            .publicId(m.getPublicId())
                            .type(m.getMediaType())
                            .build());
        }

        List<PostResponse> result = flatPage.getContent()
                .stream()
                .map(row -> PostResponse.builder()
                        .id(row.getId())
                        .content(row.getContent())
                        .visibility(row.getVisibility())
                        .createdAt(row.getCreatedAt())
                        .userId(row.getUserId())
                        .username(row.getUsername())
                        .avatarUrl(row.getAvatarUrl())
                        .postMediaResponses(
                                mediaMap.getOrDefault(
                                        row.getId(),
                                        Collections.emptyList()
                                )
                        )
                        .build())
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

        log.info(
                "POST_EVENT | action=CREATE_POST | userId={} | postId={}",
                userId,
                post.getId()
        );
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

        postServiceDomain.checkOwner(post);

        boolean emptyContent =
                request.getContent() == null ||
                        request.getContent().isBlank();

        long mediaCount = postMediaRepository.countByPostId(postId);

        if (emptyContent && mediaCount == 0) {
            throw new IllegalArgumentException(
                    "Post must contain content or media."
            );
        }

        post.setContent(request.getContent());
        post.setVisibility(request.getVisibility());

        updatePostHashtags(post, request.getContent());
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

        postServiceDomain.checkOwner(post);

        postMediaRepository.findByPostId(postId)
                .forEach(media ->
                        cloudinaryService.deleteImage(media.getPublicId()));

        postMediaRepository.deleteByPostId(postId);

        deleteUnusedHashtags(postId);

        postRepository.delete(post);

        log.info(
                "POST_EVENT | action=DELETE_POST | userId={} | postId={}",
                userId,
                postId
        );
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

        postServiceDomain.checkOwner(post);

        savePostMedia(post, request.getFiles());
    }

    @Override
    @Transactional
    public void deletePostMedia(String publicId) {

        PostMedia media = postMediaRepository.findByPublicId(publicId)
                .orElseThrow(() ->
                        new MediaNotFoundException("Media not found: " + publicId)
                );

        Post post = media.getPost();

        postServiceDomain.checkOwner(post);

        long totalMedia = postMediaRepository.countByPostId(post.getId());

        boolean emptyContent =
                post.getContent() == null ||
                        post.getContent().isBlank();

        if (totalMedia == 1 && emptyContent) {
            throw new IllegalStateException(
                    "Post must contain at least one media or content."
            );
        }

        cloudinaryService.deleteImage(publicId);

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

        files.forEach(cloudinaryService::validateImage);

        List<PostMedia> mediaList =
                new ArrayList<>(files.size());

        for (MultipartFile file : files) {

            UploadFileResponse upload =
                    cloudinaryService.uploadImage(file, "posts");

            mediaList.add(PostMedia.builder()
                    .post(post)
                    .mediaType(MediaType.IMAGE)
                    .url(upload.getImageUrl())
                    .publicId(upload.getPublicId())
                    .build());
        }

        postMediaRepository.saveAll(mediaList);
    }
}