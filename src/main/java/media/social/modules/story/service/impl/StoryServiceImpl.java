package media.social.modules.story.service.impl;

import lombok.RequiredArgsConstructor;
import media.social.modules.auth.Enum.Status;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.file.image.dto.response.UploadFileResponse;
import media.social.modules.file.image.service.CloudinaryService;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.story.dto.projection.StoryFeedProjection;
import media.social.modules.story.dto.projection.StoryInteractionProjection;
import media.social.modules.story.dto.projection.UserStoryProjection;
import media.social.modules.story.dto.request.CreateStoryRequest;
import media.social.modules.story.dto.request.UpdateStoryVisibilityRequest;
import media.social.modules.story.dto.response.MyStoryResponse;
import media.social.modules.story.dto.response.StoryFeedResponse;
import media.social.modules.story.dto.response.StoryViewAndReaction;
import media.social.modules.story.dto.response.UserStoryResponse;
import media.social.modules.story.entity.Story;
import media.social.modules.story.entity.StoryMedia;
import media.social.modules.story.exception.story.StoryForbiddenException;
import media.social.modules.story.exception.story.StoryNotFoundException;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.story.repository.StoryViewRepository;
import media.social.modules.story.service.StoryService;
import media.social.modules.story.service.cache.StoryCacheService;
import media.social.modules.user.entity.User;
import media.social.modules.user.enums.FriendshipStatus;
import media.social.modules.user.service.domain.FriendShipDomain;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StoryServiceImpl implements StoryService {

    private final StoryRepository storyRepository;
    private final CloudinaryService cloudinaryService;
    private final UserServiceDomain userServiceDomain;
    private final FriendShipDomain friendShipDomain;
    private final StoryCacheService storyCacheService;
    private final StoryViewRepository storyViewRepository;

    @Override
    @Transactional
    public void create(CreateStoryRequest request) {

        Long userId = UserContextHolder.getUserId();

        User user = userServiceDomain.getByUserId(userId);

        Story story = Story.builder()
                .user(user)
                .content(request.getContent())
                .visibility(request.getVisibility())
                .build();

        MultipartFile file = request.getFile();

        if (file != null && !file.isEmpty()) {

            MediaType mediaType =
                    cloudinaryService.detectMediaType(file);

            cloudinaryService.validateFile(
                    file,
                    mediaType
            );

            UploadFileResponse uploadResponse =
                    cloudinaryService.uploadFile(
                            file,
                            "stories/" + userId,
                            mediaType
                    );

            StoryMedia media = StoryMedia.builder()
                    .story(story)
                    .url(uploadResponse.getFileUrl())
                    .publicId(uploadResponse.getPublicId())
                    .mediaType(mediaType)
                    .build();

            story.setMedia(media);
        }
        storyCacheService.evictUserStories(userId);

        Story savedStory = storyRepository.save(story);

    }

    @Override
    @Transactional(readOnly = true)
    public List<StoryFeedResponse> getFeed() {

        Long viewerId = UserContextHolder.getUserId();

        List<StoryFeedProjection> projections =
                storyRepository.findFeed(
                        viewerId,
                        LocalDateTime.now(),
                        FriendshipStatus.ACCEPTED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        Status.ACTIVE
                );

        return projections.stream()
                .map(row -> StoryFeedResponse.builder()
                        .userId(row.getUserId())
                        .avatarUrl(row.getAvatarUrl())
                        .content(row.getContent())
                        .visibility(row.getVisibility())
                        .mediaType(row.getMediaType())
                        .url(row.getUrl())
                        .build())
                .toList();
    }

    @Override
    public List<UserStoryResponse> getUserStoriesBeforeExpire(Long targetUserId) {

        Long viewerId = UserContextHolder.getUserId();

        List<UserStoryProjection> projections =
                storyRepository.findUserStories(
                        viewerId,
                        targetUserId,
                        LocalDateTime.now(),
                        FriendshipStatus.ACCEPTED,
                        Visibility.PUBLIC,
                        Visibility.FRIEND,
                        Status.ACTIVE
                );

        return projections.stream()
                .map(row -> UserStoryResponse.builder()
                        .storyId(row.getStoryId())
                        .userId(row.getUserId())
                        .username(row.getUsername())
                        .avatarUrl(row.getAvatarUrl())
                        .content(row.getContent())
                        .visibility(row.getVisibility())
                        .mediaType(row.getMediaType())
                        .url(row.getUrl())
                        .expiresAt(row.getExpiresAt())
                        .createdAt(row.getCreatedAt())
                        .updatedAt(row.getUpdatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStoryResponse> getUserStories(
            Long targetUserId
    ) {

        Long viewerId = UserContextHolder.getUserId();

        List<UserStoryResponse> stories =
                storyCacheService.getUserStories(targetUserId);

        if (viewerId.equals(targetUserId)) {
            return stories;
        }

        boolean isFriend =
                friendShipDomain.areFriends(
                        viewerId,
                        targetUserId
                );

        if (isFriend) {

            return stories.stream()
                    .filter(story ->
                            Visibility.PUBLIC
                                    .equals(story.getVisibility())
                                    ||
                                    Visibility.FRIEND
                                            .equals(story.getVisibility())
                    )
                    .toList();
        }

        return stories.stream()
                .filter(story ->
                        Visibility.PUBLIC
                                .equals(story.getVisibility())
                )
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyStoryResponse> getMyStories() {

        Long userId = UserContextHolder.getUserId();

        List<UserStoryResponse> stories =
                storyCacheService.getUserStories(userId);

        if (stories.isEmpty()) {
            return List.of();
        }

        List<Long> storyIds = stories.stream()
                .map(UserStoryResponse::getStoryId)
                .toList();

        List<StoryInteractionProjection> interactionProjections =
                storyViewRepository.findInteractionsByStoryIds(storyIds);

        Map<Long, List<StoryInteractionProjection>> interactionMap =
                interactionProjections.stream()
                        .collect(Collectors.groupingBy(
                                StoryInteractionProjection::getStoryId
                        ));

        return stories.stream()
                .map(story -> {

                    List<StoryViewAndReaction> interactions =
                            interactionMap
                                    .getOrDefault(
                                            story.getStoryId(),
                                            List.of()
                                    )
                                    .stream()
                                    .map(interaction ->
                                            StoryViewAndReaction.builder()
                                                    .userId(interaction.getUserId())
                                                    .username(interaction.getUsername())
                                                    .avatarUrl(interaction.getAvatarUrl())
                                                    .viewAt(interaction.getViewAt())
                                                    .reactionType(interaction.getReactionType())
                                                    .reactionAt(interaction.getReactionAt())
                                                    .build()
                                    )
                                    .toList();

                    return MyStoryResponse.builder()
                            .storyId(story.getStoryId())
                            .userId(story.getUserId())
                            .username(story.getUsername())
                            .avatarUrl(story.getAvatarUrl())
                            .content(story.getContent())
                            .visibility(story.getVisibility())
                            .mediaType(story.getMediaType())
                            .url(story.getUrl())
                            .interactions(interactions)
                            .expiresAt(story.getExpiresAt())
                            .createdAt(story.getCreatedAt())
                            .updatedAt(story.getUpdatedAt())
                            .build();
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateVisibility(
            Long storyId,
            UpdateStoryVisibilityRequest request
    ) {

        Long userId = UserContextHolder.getUserId();

        Story story = storyRepository.findByIdWithUserAndMedia(storyId).orElseThrow(
                () -> new StoryNotFoundException(
                        "Story not found"
                )
        );

        if (!story.getUser().getId().equals(userId)) {
            throw new StoryForbiddenException(
                    "You do not have permission to update this story"
            );
        }

        story.setVisibility(request.getVisibility());

        storyRepository.save(story);

        storyCacheService.evictUserStories(userId);

    }

    @Override
    @Transactional
    public void delete(Long storyId) {

        Long userId = UserContextHolder.getUserId();

        Story story = storyRepository.findByIdWithUserAndMedia(storyId).orElseThrow(
                () -> new StoryNotFoundException(
                        "Story not found"
                )
        );;

        if (!story.getUser().getId().equals(userId)) {
            throw new StoryForbiddenException(
                    "You do not have permission to delete this story"
            );
        }

        StoryMedia media = story.getMedia();

        if (media != null) {
            cloudinaryService.deleteFile(
                    media.getPublicId(),
                    media.getMediaType()
            );
        }

        storyRepository.delete(story);

        storyCacheService.evictUserStories(userId);
    }
}