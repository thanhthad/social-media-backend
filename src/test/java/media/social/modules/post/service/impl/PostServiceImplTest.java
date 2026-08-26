package media.social.modules.post.service.impl;

import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.file.dto.response.UploadFileResponse;
import media.social.modules.file.media.upload.MediaUploadContext;
import media.social.modules.file.media.upload.service.MediaUploadService;
import media.social.modules.post.dto.projection.ListPostMediaProjection;
import media.social.modules.post.dto.projection.PostFlatProjection;
import media.social.modules.post.dto.request.post.CreatePostRequest;
import media.social.modules.post.dto.request.post.UpdatePostContent;
import media.social.modules.post.dto.request.post.UpdatePostMedia;
import media.social.modules.post.dto.request.post.UpdatePostVisibility;
import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.dto.response.post.PostResponse;
import media.social.modules.post.entity.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.exception.post.PostNotFoundException;
import media.social.modules.post.exception.post_media.MediaNotFoundException;
import media.social.modules.post.repository.*;
import media.social.modules.post.service.cache.PostCacheService;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.exception.block.UserBlockedException;
import media.social.modules.user.service.domain.BlockPolicyService;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @InjectMocks
    private PostServiceImpl postService;

    @Mock private PostRepository postRepository;
    @Mock private PostMediaRepository postMediaRepository;
    @Mock private UserServiceDomain userServiceDomain;
    @Mock private MediaUploadService mediaUploadService;
    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostHashtagRepository postHashtagRepository;
    @Mock private BlockPolicyService blockPolicyService;
    @Mock private FriendShipDomain friendShipDomain;
    @Mock private PostDomainService postDomainService;
    @Mock private ReactionRepository reactionRepository;
    @Mock private PostCacheService postCacheService;

    // ==========================================
    // FEED & EXPLORE TESTS
    // ==========================================

    @Test
    void getFeed_success() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        PostFlatProjection flat = mock(PostFlatProjection.class);
        when(flat.getId()).thenReturn(10L);
        when(flat.getContent()).thenReturn("Hello");
        when(flat.getVisibility()).thenReturn(Visibility.PUBLIC);
        when(flat.getCommentCount()).thenReturn(0L);
        when(flat.getReactionCount()).thenReturn(0L);

        Page<PostFlatProjection> flatPage = new PageImpl<>(List.of(flat));

        ListPostMediaProjection media = mock(ListPostMediaProjection.class);
        when(media.getPostId()).thenReturn(10L);
        when(media.getPostMediaId()).thenReturn(100L);
        when(media.getUrl()).thenReturn("http://media.url");
        when(media.getType()).thenReturn(MediaType.IMAGE);

        Reaction reaction = Reaction.builder()
                .post(Post.builder().id(10L).build())
                .type(ReactionType.LIKE)
                .build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findFeed(viewerId, Status.ACTIVE, PostType.POST, ReportStatus.APPROVED, FriendshipStatus.ACCEPTED, Visibility.PUBLIC, Visibility.FRIEND, pageable))
                    .thenReturn(flatPage);
            when(postMediaRepository.findMediaByPostIds(List.of(10L))).thenReturn(List.of(media));
            when(reactionRepository.findMyReactions(viewerId, List.of(10L))).thenReturn(List.of(reaction));

            Page<PostResponse> result = postService.getFeed(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            PostResponse postResponse = result.getContent().get(0);
            assertEquals(10L, postResponse.getId());
            assertEquals("Hello", postResponse.getContent());
            assertTrue(postResponse.isReacted());
            assertEquals(ReactionType.LIKE, postResponse.getMyReactionType());
            assertEquals(1, postResponse.getPostMediaResponses().size());
        }
    }

    @Test
    void getFeed_empty_returnsEmptyPage() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostFlatProjection> emptyFlatPage = new PageImpl<>(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findFeed(viewerId, Status.ACTIVE, PostType.POST, ReportStatus.APPROVED, FriendshipStatus.ACCEPTED, Visibility.PUBLIC, Visibility.FRIEND, pageable))
                    .thenReturn(emptyFlatPage);

            Page<PostResponse> result = postService.getFeed(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void getExplore_success() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostFlatProjection> flatPage = new PageImpl<>(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findExplore(viewerId, Status.ACTIVE, PostType.POST, ReportStatus.APPROVED, Visibility.PUBLIC, FriendshipStatus.ACCEPTED, pageable))
                    .thenReturn(flatPage);

            Page<PostResponse> result = postService.getExplore(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void getAllPostMe_success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostFlatProjection> flatPage = new PageImpl<>(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.findAllPostMe(userId, Status.ACTIVE, ReportStatus.APPROVED, PostType.POST, pageable)).thenReturn(flatPage);

            Page<PostResponse> result = postService.getAllPostMe(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void getAllPostByUserId_isBlocked_throwsUserBlockedException() {
        Long viewerId = 1L;
        Long targetUserId = 2L;
        Pageable pageable = PageRequest.of(0, 10);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(blockPolicyService.isBlocked(viewerId, targetUserId)).thenReturn(true);

            assertThrows(UserBlockedException.class, () -> postService.getAllPostByUserId(targetUserId, pageable));
        }
    }

    @Test
    void getAllPostByUserId_success() {
        Long viewerId = 1L;
        Long targetUserId = 2L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostFlatProjection> flatPage = new PageImpl<>(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(blockPolicyService.isBlocked(viewerId, targetUserId)).thenReturn(false);
            when(postRepository.findAllVisiblePost(viewerId, targetUserId, Status.ACTIVE, ReportStatus.APPROVED, Visibility.PUBLIC, Visibility.FRIEND, FriendshipStatus.ACCEPTED, PostType.POST, pageable))
                    .thenReturn(flatPage);

            Page<PostResponse> result = postService.getAllPostByUserId(targetUserId, pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================
    // GET POST BY ID TESTS
    // ==========================================

    @Test
    void getPostById_isBlocked_throwsUserBlockedException() {
        Long viewerId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        Post post = Post.builder().id(postId).user(User.builder().id(ownerId).build()).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findByIdWithUser(postId, PostType.POST)).thenReturn(Optional.of(post));
            when(blockPolicyService.isBlocked(viewerId, ownerId)).thenReturn(true);

            assertThrows(UserBlockedException.class, () -> postService.getPostById(postId));
        }
    }

    @Test
    void getPostById_notFound_throwsPostNotFoundException() {
        Long viewerId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findByIdWithUser(postId, PostType.POST)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.getPostById(postId));
        }
    }

    @Test
    void getPostById_ownerViewing_hasFullVisibilities() {
        Long userId = 1L;
        Long postId = 10L;

        Post post = Post.builder().id(postId).user(User.builder().id(userId).build()).build();
        PostCacheDTO cacheDTO = PostCacheDTO.builder().id(postId).content("Post content").build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.findByIdWithUser(postId, PostType.POST)).thenReturn(Optional.of(post));
            when(blockPolicyService.isBlocked(userId, userId)).thenReturn(false);
            when(postCacheService.getPost(eq(postId), eq(List.of(Visibility.PUBLIC, Visibility.FRIEND, Visibility.PRIVATE))))
                    .thenReturn(cacheDTO);
            when(reactionRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());

            PostResponse result = postService.getPostById(postId);

            assertNotNull(result);
            assertEquals(postId, result.getId());
            assertEquals("Post content", result.getContent());
            assertFalse(result.isReacted());
        }
    }

    @Test
    void getPostById_friendViewing_hasPublicAndFriendVisibilities() {
        Long viewerId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        Post post = Post.builder().id(postId).user(User.builder().id(ownerId).build()).build();
        PostCacheDTO cacheDTO = PostCacheDTO.builder().id(postId).content("Friend post").build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findByIdWithUser(postId, PostType.POST)).thenReturn(Optional.of(post));
            when(blockPolicyService.isBlocked(viewerId, ownerId)).thenReturn(false);
            when(friendShipDomain.areFriends(viewerId, ownerId)).thenReturn(true);
            when(postCacheService.getPost(eq(postId), eq(List.of(Visibility.PUBLIC, Visibility.FRIEND))))
                    .thenReturn(cacheDTO);
            when(reactionRepository.findByUserIdAndPostId(viewerId, postId)).thenReturn(Optional.empty());

            PostResponse result = postService.getPostById(postId);

            assertNotNull(result);
            assertEquals(postId, result.getId());
        }
    }

    @Test
    void getPostById_strangerViewing_hasPublicVisibilityOnly() {
        Long viewerId = 1L;
        Long ownerId = 2L;
        Long postId = 10L;

        Post post = Post.builder().id(postId).user(User.builder().id(ownerId).build()).build();
        PostCacheDTO cacheDTO = PostCacheDTO.builder().id(postId).content("Public post").build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.findByIdWithUser(postId, PostType.POST)).thenReturn(Optional.of(post));
            when(blockPolicyService.isBlocked(viewerId, ownerId)).thenReturn(false);
            when(friendShipDomain.areFriends(viewerId, ownerId)).thenReturn(false);
            when(postCacheService.getPost(eq(postId), eq(List.of(Visibility.PUBLIC))))
                    .thenReturn(cacheDTO);
            when(reactionRepository.findByUserIdAndPostId(viewerId, postId)).thenReturn(Optional.empty());

            PostResponse result = postService.getPostById(postId);

            assertNotNull(result);
            assertEquals(postId, result.getId());
        }
    }

    // ==========================================
    // SEARCH TESTS
    // ==========================================

    @Test
    void searchByContent_success() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        PostFlatProjection projection = mock(PostFlatProjection.class);
        when(projection.getId()).thenReturn(10L);
        when(projection.getContent()).thenReturn("search text");
        when(projection.getVisibility()).thenReturn(Visibility.PUBLIC);

        Page<PostFlatProjection> projectionPage = new PageImpl<>(List.of(projection));

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.searchByContent(viewerId, "search", Status.ACTIVE.name(), PostType.POST.name(), ReportStatus.APPROVED.name(),
                    Visibility.PUBLIC.name(), Visibility.FRIEND.name(), FriendshipStatus.ACCEPTED.name(), pageable))
                    .thenReturn(projectionPage);
            when(postMediaRepository.findMediaByPostIds(List.of(10L))).thenReturn(Collections.emptyList());
            when(reactionRepository.findMyReactions(viewerId, List.of(10L))).thenReturn(Collections.emptyList());

            Page<PostResponse> result = postService.searchByContent(" search ", pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(10L, result.getContent().get(0).getId());
        }
    }

    @Test
    void searchByHashtag_success() {
        Long viewerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostFlatProjection> flatPage = new PageImpl<>(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(viewerId);

            when(postRepository.searchByHashtag(viewerId, "java", Status.ACTIVE, PostType.POST, ReportStatus.APPROVED,
                    Visibility.PUBLIC, Visibility.FRIEND, FriendshipStatus.ACCEPTED, pageable))
                    .thenReturn(flatPage);

            Page<PostResponse> result = postService.searchByHashtag(" Java ", pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Test
    void getAllSavedPost_success() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<PostFlatProjection> flatPage = new PageImpl<>(Collections.emptyList());

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.findSavedPosts(userId, Status.ACTIVE, PostType.POST, ReportStatus.APPROVED,
                    Visibility.PUBLIC, Visibility.FRIEND, FriendshipStatus.ACCEPTED, pageable))
                    .thenReturn(flatPage);

            Page<PostResponse> result = postService.getAllSavedPost(pageable);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==========================================
    // CREATE, UPDATE, DELETE POST TESTS
    // ==========================================

    @Test
    void createPost_success_withFilesAndHashtags() {
        Long userId = 1L;
        MultipartFile file = mock(MultipartFile.class);

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Check out this #Java and #SpringBoot post!");
        request.setVisibility(Visibility.PUBLIC);
        request.setFiles(List.of(file));

        User user = new User();
        user.setId(userId);

        UploadFileResponse uploadResponse = UploadFileResponse.builder()
                .fileUrl("http://file.url")
                .publicId("pid")
                .resourceType("IMAGE")
                .build();

        Hashtag javaHashtag = Hashtag.builder().hashtagId(1L).name("java").build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);
            when(mediaUploadService.upload(file, MediaUploadContext.POST)).thenReturn(uploadResponse);

            when(hashtagRepository.findByName("java")).thenReturn(Optional.of(javaHashtag));
            when(hashtagRepository.findByName("springboot")).thenReturn(Optional.empty());
            when(hashtagRepository.save(any(Hashtag.class))).thenAnswer(i -> i.getArgument(0));

            postService.createPost(request);

            verify(postRepository).save(any(Post.class));
            verify(postMediaRepository).saveAll(anyList());
            verify(postHashtagRepository).saveAll(anyList());
        }
    }

    @Test
    void updatePostContent_success() {
        Long postId = 10L;
        UpdatePostContent request = new UpdatePostContent();
        request.setContent("New content #Updated");

        Post post = Post.builder().id(postId).content("Old content").build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postMediaRepository.countByPostId(postId)).thenReturn(0L);
        when(postHashtagRepository.findByPost_Id(postId)).thenReturn(Collections.emptyList());
        when(hashtagRepository.findByName("updated")).thenReturn(Optional.empty());
        when(hashtagRepository.save(any(Hashtag.class))).thenAnswer(i -> i.getArgument(0));

        postService.updatePostContent(postId, request);

        assertEquals("New content #Updated", post.getContent());
        verify(postDomainService).checkOwner(post);
    }

    @Test
    void updatePostContent_notFound_throwsPostNotFoundException() {
        Long postId = 10L;
        UpdatePostContent request = new UpdatePostContent();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.updatePostContent(postId, request));
    }

    @Test
    void updatePostContent_emptyContentAndNoMedia_throwsIllegalArgumentException() {
        Long postId = 10L;
        UpdatePostContent request = new UpdatePostContent();
        request.setContent("   ");

        Post post = Post.builder().id(postId).content("Old content").build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postMediaRepository.countByPostId(postId)).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> postService.updatePostContent(postId, request));
    }

    @Test
    void updatePostVisibility_success() {
        Long postId = 10L;
        UpdatePostVisibility request = new UpdatePostVisibility();
        request.setVisibility(Visibility.PRIVATE);

        Post post = Post.builder().id(postId).visibility(Visibility.PUBLIC).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.updatePostVisibility(postId, request);

        assertEquals(Visibility.PRIVATE, post.getVisibility());
        verify(postDomainService).checkOwner(post);
    }

    @Test
    void updatePostVisibility_notFound_throwsPostNotFoundException() {
        Long postId = 10L;
        UpdatePostVisibility request = new UpdatePostVisibility();
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.updatePostVisibility(postId, request));
    }

    @Test
    void deleteByPostId_success() {
        Long userId = 1L;
        Long postId = 10L;

        Post post = Post.builder().id(postId).build();
        PostMedia media = PostMedia.builder().publicId("pid").mediaType(MediaType.IMAGE).build();
        Hashtag hashtag = Hashtag.builder().hashtagId(100L).build();
        PostHashtag postHashtag = PostHashtag.builder().hashtag(hashtag).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            when(postMediaRepository.findByPostId(postId)).thenReturn(List.of(media));
            when(postHashtagRepository.findByPost_Id(postId)).thenReturn(List.of(postHashtag));

            postService.deleteByPostId(postId);

            verify(postDomainService).checkOwner(post);
            verify(mediaUploadService).delete("pid", MediaType.IMAGE);
            verify(postMediaRepository).deleteByPostId(postId);
            verify(postHashtagRepository).delete(postHashtag);
            verify(hashtagRepository).deleteIfUnused(100L);
            verify(postRepository).delete(post);
        }
    }

    @Test
    void deleteByPostId_notFound_throwsPostNotFoundException() {
        Long userId = 1L;
        Long postId = 10L;

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThrows(PostNotFoundException.class, () -> postService.deleteByPostId(postId));
        }
    }

    @Test
    void updatePostMedia_success() {
        Long postId = 10L;
        MultipartFile file = mock(MultipartFile.class);
        UpdatePostMedia request = new UpdatePostMedia();
        request.setFiles(List.of(file));

        Post post = Post.builder().id(postId).build();
        UploadFileResponse uploadResponse = UploadFileResponse.builder().fileUrl("url").publicId("pid").resourceType("IMAGE").build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(mediaUploadService.upload(file, MediaUploadContext.POST)).thenReturn(uploadResponse);

        postService.updatePostMedia(postId, request);

        verify(postDomainService).checkOwner(post);
        verify(postMediaRepository).saveAll(anyList());
    }

    @Test
    void updatePostMedia_noFiles_throwsIllegalArgumentException() {
        Long postId = 10L;
        UpdatePostMedia request = new UpdatePostMedia();
        request.setFiles(Collections.emptyList());

        assertThrows(IllegalArgumentException.class, () -> postService.updatePostMedia(postId, request));
    }

    @Test
    void deletePostMedia_success() {
        Long mediaId = 100L;
        Post post = Post.builder().id(10L).content("Some content").build();
        PostMedia media = PostMedia.builder()
                .id(mediaId)
                .post(post)
                .publicId("pid")
                .mediaType(MediaType.IMAGE)
                .build();

        when(postMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(postMediaRepository.countByPostId(10L)).thenReturn(2L);

        postService.deletePostMedia(mediaId);

        verify(postDomainService).checkOwner(post);
        verify(mediaUploadService).delete("pid", MediaType.IMAGE);
        verify(postMediaRepository).delete(media);
    }

    @Test
    void deletePostMedia_notFound_throwsMediaNotFoundException() {
        Long mediaId = 100L;
        when(postMediaRepository.findById(mediaId)).thenReturn(Optional.empty());

        assertThrows(MediaNotFoundException.class, () -> postService.deletePostMedia(mediaId));
    }

    @Test
    void deletePostMedia_lastMediaAndEmptyContent_throwsIllegalStateException() {
        Long mediaId = 100L;
        Post post = Post.builder().id(10L).content("   ").build();
        PostMedia media = PostMedia.builder()
                .id(mediaId)
                .post(post)
                .build();

        when(postMediaRepository.findById(mediaId)).thenReturn(Optional.of(media));
        when(postMediaRepository.countByPostId(10L)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> postService.deletePostMedia(mediaId));
        verify(postMediaRepository, never()).delete(any());
    }

    // ==========================================
    // ADDITIONAL BUSINESS LOGIC TESTS
    // ==========================================

    @Test
    void createPost_textOnly_noFiles_success() {
        Long userId = 1L;

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("Text only post, no media.");
        request.setVisibility(Visibility.PUBLIC);
        request.setFiles(null);

        User user = new User();
        user.setId(userId);

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(userServiceDomain.getByUserId(userId)).thenReturn(user);

            postService.createPost(request);

            verify(postRepository).save(any(Post.class));
            verify(postMediaRepository, never()).saveAll(anyList());
        }
    }

    @Test
    void updatePostContent_blankContentButHasMedia_success() {
        Long postId = 10L;
        UpdatePostContent request = new UpdatePostContent();
        request.setContent("   ");

        Post post = Post.builder().id(postId).content("Old content").build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(postMediaRepository.countByPostId(postId)).thenReturn(2L);
        when(postHashtagRepository.findByPost_Id(postId)).thenReturn(Collections.emptyList());

        postService.updatePostContent(postId, request);

        assertEquals("   ", post.getContent());
        verify(postDomainService).checkOwner(post);
    }

    @Test
    void updatePostMedia_notFound_throwsPostNotFoundException() {
        Long postId = 10L;
        MultipartFile file = mock(MultipartFile.class);
        UpdatePostMedia request = new UpdatePostMedia();
        request.setFiles(List.of(file));

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> postService.updatePostMedia(postId, request));
        verify(postMediaRepository, never()).saveAll(anyList());
    }

    @Test
    void updatePostContent_notOwner_throwsForbiddenException() {
        Long postId = 10L;
        UpdatePostContent request = new UpdatePostContent();
        request.setContent("Hacked content");

        Post post = Post.builder().id(postId).content("Original").build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        doThrow(new media.social.modules.post.exception.post.ForbiddenException("Not owner"))
                .when(postDomainService).checkOwner(post);

        assertThrows(media.social.modules.post.exception.post.ForbiddenException.class,
                () -> postService.updatePostContent(postId, request));
    }

    @Test
    void deleteByPostId_notOwner_throwsForbiddenException() {
        Long userId = 1L;
        Long postId = 10L;

        Post post = Post.builder().id(postId).build();

        try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
            mockedContext.when(UserContextHolder::getUserId).thenReturn(userId);

            when(postRepository.findById(postId)).thenReturn(Optional.of(post));
            doThrow(new media.social.modules.post.exception.post.ForbiddenException("Not owner"))
                    .when(postDomainService).checkOwner(post);

            assertThrows(media.social.modules.post.exception.post.ForbiddenException.class,
                    () -> postService.deleteByPostId(postId));
            verify(postRepository, never()).delete(any());
        }
    }
}
