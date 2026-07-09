package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.post.enums.MediaType;
import media.social.modults.post.dto.request.post.CreatePostRequest;
import media.social.modults.post.dto.request.post.UpdatePostContent;
import media.social.modults.post.dto.request.post.UpdatePostMedia;
import media.social.modults.post.dto.response.post.PostFlatResponse;
import media.social.modults.post.dto.response.post.PostMediaResponse;
import media.social.modults.post.dto.response.post.PostResponse;
import media.social.modults.file.image.dto.response.UploadImageResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import media.social.modults.post.enums.Visibility;
import media.social.modults.post.exception.post_media.MediaNotFoundException;
import media.social.modults.post.repository.HashtagRepository;
import media.social.modults.post.repository.PostHashtagRepository;
import media.social.modults.post.repository.PostMediaRepository;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.entity.Profile;
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
    @Transactional
    public void createPost(CreatePostRequest request) {

        boolean emptyContent =
                request.getContent() == null ||
                        request.getContent().isBlank();

        boolean emptyFiles =
                request.getFiles() == null ||
                        request.getFiles().isEmpty();

        if (emptyContent && emptyFiles) {
            throw new IllegalArgumentException(
                    "Post must contain content or at least one media."
            );
        }

        Long userId = UserContextHolder.getUserId();

        User user = userServiceDomain.getByUserId(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .build();

        postRepository.save(post);

        savePostMedia(post, request.getFiles());

        log.info(
                "POST_EVENT | action=CREATE_POST | userId={} | postId={}",
                userId,
                post.getId()
        );
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

    @Override
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PostNotFoundException("Post not found")
        );
        User user = userServiceDomain.getByUserId(UserContextHolder.getUserId());
        Profile profile = profileRepo.
        List<PostMedia> postMedias = postMediaRepository.findByPostId(postId);
        List<PostMediaResponse> postMediaResponses = new ArrayList<>();
        for(PostMedia postMedia : postMedias){
            PostMediaResponse postMediaResponse = PostMediaResponse.builder()
                    .url(postMedia.getUrl())
                    .publicId(postMedia.getPublicId())
                    .type(postMedia.getMediaType())
            .build();
            postMediaResponses.add(postMediaResponse);
        }
        return PostResponse.builder()
                .id(postId)
                .content(post.getContent())
                .visibility(post.getVisibility())
                .createdAt(post.getCreatedAt())
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl()
                .build();
    }

    @Transactional(readOnly = true)
    private Page<PostResponse> getAllPost(Long userId,List<Visibility> visibilities, Pageable pageable) {

        Page<PostFlatResponse> flatPage =
                postRepository.findAllVisiblePost(userId,visibilities, pageable);

        return buildPostResponse(flatPage,pageable);
    }

    @Transactional(readOnly = true)
    private Page<PostResponse> getAllPost(Long userId, Pageable pageable) {

        Page<PostFlatResponse> flatPage =
                postRepository.findAllPostMe(userId, pageable);

        return buildPostResponse(flatPage,pageable);
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
                            .type(m.getMediaType().name())
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

        long mediaCount =
                postMediaRepository.countByPostId(postId);

        if (emptyContent && mediaCount == 0) {
            throw new IllegalArgumentException(
                    "Post must contain content or media."
            );
        }

        post.setContent(request.getContent());
        post.setVisibility(request.getVisibility());
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


    @Transactional
    @Override
    public void deleteByPostId(Long postId) {
        Long userId = UserContextHolder.getUserId();

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PostNotFoundException("Post not found with postId: " + postId)
        );
        postServiceDomain.checkOwner(post);
        List<PostMedia>  postMediaList = postMediaRepository.findByPostId(postId);

        postMediaList.forEach(media ->
                cloudinaryService.deleteImage(media.getPublicId()));

        postRepository.delete(post);

        log.info("POST_EVENT | action=DELETE_POST | UserId={} | postId={} | status=SUCCESS",
                userId, postId);
    }

    private void savePostMedia(Post post, List<MultipartFile> files) {

        if (files == null || files.isEmpty()) {
            return;
        }

        files.forEach(cloudinaryService::validateImage);

        List<PostMedia> mediaList = new ArrayList<>();

        for (MultipartFile file : files) {

            UploadImageResponse upload =
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