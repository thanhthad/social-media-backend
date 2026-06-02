package media.social.modults.post.service.impl;

import jakarta.transaction.Transactional;
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
import org.springframework.web.multipart.MultipartFile;

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


    @Override
    public PostResponse createPost(CreatePostRequest request) {

        Long userId = UserContextHolder.getUserId();
        User user = userServiceDomain.getByUserId(userId);

        Post post = Post.builder()
                .user(user)
                .content(request.getContent())
                .build();

        Post savedPost = postRepository.save(post);

        List<PostMedia> mediaList = new ArrayList<>();
        List<PostMediaResponse> mediaResponseList = new ArrayList<>();

        for (MultipartFile file : request.getFiles()) {

            cloudinaryService.validateImage(file);

            UploadImageResponse upload =
                    cloudinaryService.uploadImage(file, "posts");

            PostMedia media = PostMedia.builder()
                    .post(savedPost)
                    .mediaType(MediaType.IMAGE)
                    .url(upload.getImageUrl())
                    .publicId(upload.getPublicId())
                    .build();

            mediaList.add(media);

            mediaResponseList.add(PostMediaResponse.builder()
                    .url(upload.getImageUrl())
                    .publicId(upload.getPublicId())
                    .type(MediaType.IMAGE.name())
                    .build());
        }

        postMediaRepository.saveAll(mediaList);

        savedPost.setMedia(mediaList);

        log.info("POST_EVENT | action=CREATE_POST | userId={} | postId={}",
                userId, savedPost.getId());

        return PostResponse.builder()
                .id(savedPost.getId())
                .content(savedPost.getContent())
                .createdAt(savedPost.getCreatedAt())
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getProfile().getAvatarUrl())
                .imageUrl(mediaResponseList)
                .build();
    }

    @Override
    public Page<PostResponse> getAllPostMe(Pageable pageable) {
        Long userId = UserContextHolder.getUserId();
        return getAllPost(userId,pageable);
    }

    @Override
    public Page<PostResponse> getAllPostByUserId(Long userId, Pageable pageable) {
        return getAllPost(userId,pageable);
    }

    private Page<PostResponse> getAllPost(Long userId, Pageable pageable) {

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

    @Override
    public void deletePostMedia(String publicId) {
        PostMedia postMedia = postMediaRepository.findByPublicId(publicId).orElseThrow(
                () -> new MediaNotFoundException("Media not found with publicId: " + publicId)
        );
        postMediaRepository.delete(postMedia);
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