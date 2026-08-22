package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.Visibility;
import media.social.modules.story.entity.*;
import media.social.modules.story.repository.StoryReactionRepository;
import media.social.modules.story.repository.StoryRepository;
import media.social.modules.story.repository.StoryViewRepository;
import media.social.modules.user.entity.User;
import media.social.modules.user.repository.UserRepository;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryMockService {

    private final UserRepository userRepository;
    private final StoryRepository storyRepository;
    private final StoryReactionRepository storyReactionRepository;
    private final StoryViewRepository storyViewRepository;
    private final Faker faker = new Faker(new Locale("vi"));

    @Transactional
    public void init() {
        if (storyRepository.count() >= 500) {
            log.info("Stories already initialized");
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        log.info("Generating 500 Stories with 24h expiration, reactions, and views...");

        int batchSize = 100;
        List<Story> storiesBatch = new ArrayList<>();
        List<StoryReaction> reactionsBatch = new ArrayList<>();
        List<StoryView> viewsBatch = new ArrayList<>();

        ReactionType[] rTypes = ReactionType.values();

        for (int i = 0; i < 500; i++) {
            User user = users.get(faker.number().numberBetween(0, users.size()));
            String caption = MockDataConstants.VIETNAMESE_STORY_CAPTIONS.get(
                    faker.number().numberBetween(0, MockDataConstants.VIETNAMESE_STORY_CAPTIONS.size())
            );

            LocalDateTime createdAt = LocalDateTime.now().minusHours(faker.number().numberBetween(0, 18));

            Story story = Story.builder()
                    .user(user)
                    .content(caption)
                    .visibility(faker.number().numberBetween(1, 10) > 2 ? Visibility.PUBLIC : Visibility.FRIEND)
                    .createdAt(createdAt)
                    .updatedAt(createdAt)
                    .expiresAt(createdAt.plusHours(24))
                    .build();

            StoryMedia media = StoryMedia.builder()
                    .story(story)
                    .url(MockDataConstants.getRandomStoryImageUrl())
                    .publicId("")
                    .mediaType(MediaType.IMAGE)
                    .createdAt(createdAt)
                    .build();

            story.setMedia(media);
            storiesBatch.add(story);

            if (storiesBatch.size() == batchSize) {
                storyRepository.saveAllAndFlush(storiesBatch);

                for (Story savedStory : storiesBatch) {
                    // Generate views
                    int viewCount = faker.number().numberBetween(5, 30);
                    Set<Long> viewedUserIds = new HashSet<>();
                    for (int v = 0; v < viewCount; v++) {
                        User viewer = users.get(faker.number().numberBetween(0, users.size()));
                        if (!viewer.getId().equals(savedStory.getUser().getId()) && viewedUserIds.add(viewer.getId())) {
                            StoryView view = StoryView.builder()
                                    .id(new StoryViewId(savedStory.getId(), viewer.getId()))
                                    .story(savedStory)
                                    .viewer(viewer)
                                    .viewedAt(createdAt.plusMinutes(faker.number().numberBetween(1, 120)))
                                    .build();
                            viewsBatch.add(view);
                        }
                    }

                    // Generate reactions
                    int reactionCount = faker.number().numberBetween(2, 12);
                    Set<Long> reactedUserIds = new HashSet<>();
                    for (int r = 0; r < reactionCount; r++) {
                        User reactor = users.get(faker.number().numberBetween(0, users.size()));
                        if (!reactor.getId().equals(savedStory.getUser().getId()) && reactedUserIds.add(reactor.getId())) {
                            StoryReaction reaction = StoryReaction.builder()
                                    .story(savedStory)
                                    .user(reactor)
                                    .type(rTypes[faker.number().numberBetween(0, rTypes.length)])
                                    .createdAt(createdAt.plusMinutes(faker.number().numberBetween(5, 180)))
                                    .build();
                            reactionsBatch.add(reaction);
                        }
                    }
                }

                storyViewRepository.saveAll(viewsBatch);
                storyReactionRepository.saveAll(reactionsBatch);

                viewsBatch.clear();
                reactionsBatch.clear();
                storiesBatch.clear();
                log.info("Saved batch of {} stories", batchSize);
            }
        }

        if (!storiesBatch.isEmpty()) {
            storyRepository.saveAllAndFlush(storiesBatch);
            viewsBatch.clear();
            reactionsBatch.clear();
            storiesBatch.clear();
        }

        log.info("Story generation complete");
    }
}
