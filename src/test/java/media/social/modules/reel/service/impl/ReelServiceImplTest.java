package media.social.modules.reel.service.impl;

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
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.user.service.domain.BlockPolicyService;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReelServiceImpl")
class ReelServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private PostMediaRepository postMediaRepository;
    @Mock private ReelRepository reelRepository;
    @Mock private UserServiceDomain userServiceDomain;
    @Mock private MediaUploadService mediaUploadService;
    @Mock private BlockPolicyService blockPolicyService;
    @Mock private FriendShipDomain friendShipDomain;
    @Mock private ReactionRepository reactionRepository;

    @InjectMocks
    private ReelServiceImpl reelService;

    @Test
    @DisplayName("Create reel with custom thumbnail uploads both video and thumbnail")
    void createReel_withCustomThumbnail_savesReelWithCustomThumbnail() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("testuser").build();

        MultipartFile videoFile = mock(MultipartFile.class);
        MultipartFile thumbnailFile = mock(MultipartFile.class);
        when(thumbnailFile.isEmpty()).thenReturn(false);

        CreateReelRequest request = CreateReelRequest.builder()
                .content("My first reel")
                .visibility(Visibility.PUBLIC)
                .video(videoFile)
                .thumbnail(thumbnailFile)
                .build();

        UploadFileResponse videoUpload = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/video.mp4")
                .publicId("reels/video123")
                .duration(45L)
                .width(1080)
                .height(1920)
                .build();

        UploadFileResponse thumbUpload = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/thumb.jpg")
                .publicId("reels/thumb123")
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(mediaUploadService.upload(videoFile, MediaUploadContext.REEL)).thenReturn(videoUpload);
            when(mediaUploadService.upload(thumbnailFile, MediaUploadContext.REEL)).thenReturn(thumbUpload);

            reelService.createReel(request);

            // Verify Post
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postRepository).save(postCaptor.capture());
            Post savedPost = postCaptor.getValue();
            assertThat(savedPost.getUser()).isEqualTo(user);
            assertThat(savedPost.getContent()).isEqualTo("My first reel");
            assertThat(savedPost.getVisibility()).isEqualTo(Visibility.PUBLIC);
            assertThat(savedPost.getPostType()).isEqualTo(PostType.REEL);

            // Verify PostMedia
            ArgumentCaptor<PostMedia> mediaCaptor = ArgumentCaptor.forClass(PostMedia.class);
            verify(postMediaRepository).save(mediaCaptor.capture());
            PostMedia savedMedia = mediaCaptor.getValue();
            assertThat(savedMedia.getUrl()).isEqualTo("https://cloudinary.com/video.mp4");
            assertThat(savedMedia.getPublicId()).isEqualTo("reels/video123");
            assertThat(savedMedia.getMediaType()).isEqualTo(MediaType.VIDEO);

            // Verify ReelDetail
            ArgumentCaptor<ReelDetail> reelCaptor = ArgumentCaptor.forClass(ReelDetail.class);
            verify(reelRepository).save(reelCaptor.capture());
            ReelDetail savedDetail = reelCaptor.getValue();
            assertThat(savedDetail.getDurationSeconds()).isEqualTo(45);
            assertThat(savedDetail.getWidth()).isEqualTo(1080);
            assertThat(savedDetail.getHeight()).isEqualTo(1920);
            assertThat(savedDetail.getThumbnailUrl()).isEqualTo("https://cloudinary.com/thumb.jpg");
            assertThat(savedDetail.getThumbnailPublicId()).isEqualTo("reels/thumb123");

            verify(mediaUploadService, never()).generateVideoThumbnailUrl(any());
        }
    }

    @Test
    @DisplayName("Create reel without thumbnail generates thumbnail from video frame at ~1s")
    void createReel_withoutThumbnail_generatesAutoThumbnail() {
        Long userId = 1L;
        User user = User.builder().id(userId).username("testuser").build();

        MultipartFile videoFile = mock(MultipartFile.class);

        CreateReelRequest request = CreateReelRequest.builder()
                .content("No thumbnail reel")
                .visibility(Visibility.FRIEND)
                .video(videoFile)
                .thumbnail(null)
                .build();

        UploadFileResponse videoUpload = UploadFileResponse.builder()
                .fileUrl("https://cloudinary.com/video2.mp4")
                .publicId("reels/video456")
                .duration(15L)
                .width(720)
                .height(1280)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(mediaUploadService.upload(videoFile, MediaUploadContext.REEL)).thenReturn(videoUpload);
            when(mediaUploadService.generateVideoThumbnailUrl("reels/video456"))
                    .thenReturn("https://cloudinary.com/video/upload/so_1.0/reels/video456.jpg");

            reelService.createReel(request);

            // Verify ReelDetail has auto-generated thumbnail and null thumbnailPublicId
            ArgumentCaptor<ReelDetail> reelCaptor = ArgumentCaptor.forClass(ReelDetail.class);
            verify(reelRepository).save(reelCaptor.capture());
            ReelDetail savedDetail = reelCaptor.getValue();
            assertThat(savedDetail.getDurationSeconds()).isEqualTo(15);
            assertThat(savedDetail.getWidth()).isEqualTo(720);
            assertThat(savedDetail.getHeight()).isEqualTo(1280);
            assertThat(savedDetail.getThumbnailUrl()).isEqualTo("https://cloudinary.com/video/upload/so_1.0/reels/video456.jpg");
            assertThat(savedDetail.getThumbnailPublicId()).isNull();

            // Only 1 upload (video), no custom thumbnail upload
            verify(mediaUploadService, times(1)).upload(any(), eq(MediaUploadContext.REEL));
            verify(mediaUploadService).generateVideoThumbnailUrl("reels/video456");
        }
    }

    @Test
    @DisplayName("Delete reel with custom thumbnail deletes both custom thumbnail and video")
    void deleteReel_withCustomThumbnail_deletesBoth() {
        Long userId = 1L;
        Long reelId = 100L;

        User owner = User.builder().id(userId).build();
        Post post = Post.builder().id(reelId).user(owner).postType(PostType.REEL).build();

        ReelDetail detail = ReelDetail.builder()
                .reelId(reelId)
                .post(post)
                .thumbnailPublicId("reels/custom-thumb")
                .thumbnailUrl("url")
                .build();

        PostMedia videoMedia = PostMedia.builder()
                .post(post)
                .publicId("reels/video-pub")
                .mediaType(MediaType.VIDEO)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(postRepository.findByIdWithUser(reelId, PostType.REEL)).thenReturn(Optional.of(post));
            when(reelRepository.findByPostId(reelId)).thenReturn(Optional.of(detail));
            when(postMediaRepository.findByPostId(reelId)).thenReturn(List.of(videoMedia));

            reelService.deleteReel(reelId);

            verify(mediaUploadService).delete("reels/custom-thumb", MediaType.IMAGE);
            verify(mediaUploadService).delete("reels/video-pub", MediaType.VIDEO);
            verify(reelRepository).delete(detail);
            verify(postMediaRepository).deleteByPostId(reelId);
            verify(postRepository).delete(post);
        }
    }

    @Test
    @DisplayName("Delete reel with auto thumbnail skips separate thumbnail image deletion")
    void deleteReel_withAutoThumbnail_deletesOnlyVideo() {
        Long userId = 1L;
        Long reelId = 100L;

        User owner = User.builder().id(userId).build();
        Post post = Post.builder().id(reelId).user(owner).postType(PostType.REEL).build();

        ReelDetail detail = ReelDetail.builder()
                .reelId(reelId)
                .post(post)
                .thumbnailPublicId(null)
                .thumbnailUrl("url")
                .build();

        PostMedia videoMedia = PostMedia.builder()
                .post(post)
                .publicId("reels/video-pub")
                .mediaType(MediaType.VIDEO)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);
            when(postRepository.findByIdWithUser(reelId, PostType.REEL)).thenReturn(Optional.of(post));
            when(reelRepository.findByPostId(reelId)).thenReturn(Optional.of(detail));
            when(postMediaRepository.findByPostId(reelId)).thenReturn(List.of(videoMedia));

            reelService.deleteReel(reelId);

            verify(mediaUploadService, never()).delete(any(), eq(MediaType.IMAGE));
            verify(mediaUploadService).delete("reels/video-pub", MediaType.VIDEO);
            verify(reelRepository).delete(detail);
            verify(postRepository).delete(post);
        }
    }

    @Test
    @DisplayName("Delete reel when not owner throws ReelForbiddenException")
    void deleteReel_notOwner_throwsForbiddenException() {
        Long currentUserId = 1L;
        Long ownerId = 2L;
        Long reelId = 100L;

        User owner = User.builder().id(ownerId).build();
        Post post = Post.builder().id(reelId).user(owner).postType(PostType.REEL).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(currentUserId);
            when(postRepository.findByIdWithUser(reelId, PostType.REEL)).thenReturn(Optional.of(post));

            assertThatThrownBy(() -> reelService.deleteReel(reelId))
                    .isInstanceOf(ReelForbiddenException.class);

            verify(reelRepository, never()).delete(any());
            verify(postRepository, never()).delete(any());
        }
    }
}
