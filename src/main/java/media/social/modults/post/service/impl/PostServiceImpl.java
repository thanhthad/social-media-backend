package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import media.social.modults.post.Enum.MediaType;
import media.social.modults.post.dto.request.CreatePostRequest;
import media.social.modults.post.dto.request.UpdatePostContent;
import media.social.modults.post.dto.request.UpdatePostMedia;
import media.social.modults.post.dto.response.PostFlatResponse;
import media.social.modults.post.dto.response.PostMediaResponse;
import media.social.modults.post.dto.response.PostResponse;
import media.social.modults.file.image.dto.response.UploadImageResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import media.social.modults.post.exception.post_media.MediaNotFoundException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;


@Service
@AllArgsConstructor
@Log4j2
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final UserServiceDomain userServiceDomain;
    private final PostMapper postMapper;
    private final CloudinaryService cloudinaryService;


    @Override
    @Transactional
    public void createPost(CreatePostRequest request) {

        Long userId = UserContextHolder.getUserId();
        User user = userServiceDomain.getByUserId(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .build();

        Post savedPost = postRepository.save(post);

        List<PostMedia> mediaList = new ArrayList<>();

        for(MultipartFile check : request.getFiles()){
            cloudinaryService.validateImage(check);
        }

        for (MultipartFile file : request.getFiles()) {

            UploadImageResponse upload =
                    cloudinaryService.uploadImage(file, "posts");

            PostMedia media = PostMedia.builder()
                    .post(savedPost)
                    .mediaType(MediaType.IMAGE)
                    .url(upload.getImageUrl())
                    .publicId(upload.getPublicId())
                    .build();

            mediaList.add(media);
        }

        if (!mediaList.isEmpty()) {
            postMediaRepository.saveAll(mediaList);
        }

        log.info("POST_EVENT | action=CREATE_POST | userId={} | postId={}",
                userId, savedPost.getId());

    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostMe(Pageable pageable) {
        Long userId = UserContextHolder.getUserId();
        return getAllPost(userId,pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPostByUserId(Long userId, Pageable pageable) {
        return getAllPost(userId, pageable);
    }

    @Transactional(readOnly = true)
    private Page<PostResponse> getAllPost(Long userId, Pageable pageable) {

        Page<PostFlatResponse> flatPage =
                postRepository.findAllPostMe(userId, pageable);

        List<Long> postIds = flatPage.getContent()
                .stream()
                .map(PostFlatResponse::getId)
                .toList();

        List<PostMedia> mediaList =
                postMediaRepository.findByPostIdIn(postIds);

        Map<Long, List<PostMediaResponse>> mediaMap = new HashMap<>();

        for (PostMedia m : mediaList) {
            mediaMap
                    .computeIfAbsent(m.getPost().getId(), k -> new ArrayList<>())
                    .add(PostMediaResponse.builder()
                            .url(m.getUrl())
                            .publicId(m.getPublicId())
                            .type(m.getMediaType().name())
                            .build()
                    );
        }

        List<PostResponse> result = flatPage.getContent()
                .stream()
                .map(row -> PostResponse.builder()
                        .id(row.getId())
                        .content(row.getContent())
                        .createdAt(row.getCreatedAt())
                        .userId(row.getUserId())
                        .username(row.getUsername())
                        .avatarUrl(row.getAvatarUrl())
                        .postMediaResponses(
                                mediaMap.getOrDefault(row.getId(), new ArrayList<>())
                        )
                        .build()
                )
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

        cloudinaryService.deleteImage(publicId);

        postMediaRepository.delete(media);
    }

    @Override
    @Transactional
    public void updatePostContent(Long postId, UpdatePostContent content) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PostNotFoundException("Post not found with id: "+postId)
        );
        post.setContent(content.getContent());
    }

    @Override
    @Transactional
    public void updatePostMedia(Long postId, UpdatePostMedia request) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PostNotFoundException("Post not found with id: " + postId)
        );

        List<PostMedia> mediaList = new ArrayList<>();

        for (MultipartFile file : request.getFiles()) {

            cloudinaryService.validateImage(file);

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

    @Transactional
    @Override
    public void deleteByPostId(Long postId) {
        Long userId = UserContextHolder.getUserId();

        Post post = postRepository.findById(postId).orElseThrow(
                () -> new PostNotFoundException("Post not found with postId: " + postId)
        );
        List<PostMedia>  postMediaList = postMediaRepository.findByPostId(postId);
        for(PostMedia media : postMediaList){
            cloudinaryService.deleteImage(media.getPublicId());
        }
        postRepository.delete(post);

        log.info("POST_EVENT | action=DELETE_POST | UserId={} | postId={} | status=SUCCESS",
                userId, postId);
    }
}