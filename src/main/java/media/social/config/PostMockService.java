package media.social.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import media.social.modules.post.entity.*;
import media.social.modules.post.enums.MediaType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.enums.ReportStatus;
import media.social.modules.post.enums.Visibility;
import media.social.modules.post.repository.*;
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
public class PostMockService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final SavedPostRepository savedPostRepository;
    private final ReportRepository reportRepository;
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final Faker faker = new Faker(new Locale("vi"));

    @Transactional
    public void init() {
        if (postRepository.count() >= 1000) {
            log.info("Posts already initialized");
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        log.info("Generating 1000 posts...");
        List<Post> postsBatch = new ArrayList<>();
        List<PostMedia> mediaBatch = new ArrayList<>();
        List<Hashtag> hashtags = new ArrayList<>();
        List<PostHashtag> postHashtagsBatch = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            String tag = faker.hacker().noun().replaceAll("\\s+", "");
            if (hashtagRepository.findByName(tag).isEmpty()) {
                Hashtag hashtag = Hashtag.builder().name(tag).build();
                hashtags.add(hashtagRepository.save(hashtag));
            }
        }
        if (hashtags.isEmpty()) {
            hashtags = hashtagRepository.findAll();
        }

        int batchSize = 100;
        for (int i = 0; i < 1000; i++) {
            User user = users.get(faker.number().numberBetween(0, users.size()));
            Post post = Post.builder()
                    .user(user)
                    .content(faker.lorem().paragraph(faker.number().numberBetween(1, 5)))
                    .visibility(Visibility.values()[faker.number().numberBetween(0, Visibility.values().length)])
                    .createdAt(LocalDateTime.now().minusDays(faker.number().numberBetween(0, 365)))
                    .updatedAt(LocalDateTime.now())
                    .build();
            postsBatch.add(post);

            if (postsBatch.size() == batchSize) {
                postRepository.saveAllAndFlush(postsBatch);
                for (Post savedPost : postsBatch) {
                    if (faker.bool().bool()) {
                        PostMedia media = PostMedia.builder()
                                .post(savedPost)
                                .url(faker.internet().image())
                                .publicId(UUID.randomUUID().toString())
                                .mediaType(MediaType.IMAGE)
                                .createdAt(LocalDateTime.now())
                                .build();
                        mediaBatch.add(media);
                    }

                    int tagsCount = faker.number().numberBetween(0, 4);
                    Set<Long> usedTags = new HashSet<>();
                    for (int j = 0; j < tagsCount; j++) {
                        Hashtag hashtag = hashtags.get(faker.number().numberBetween(0, hashtags.size()));
                        if (usedTags.add(hashtag.getHashtagId())) {
                            PostHashtag postHashtag = PostHashtag.builder()
                                    .post(savedPost)
                                    .hashtag(hashtag)
                                    .build();
                            postHashtagsBatch.add(postHashtag);
                        }
                    }
                }
                postMediaRepository.saveAll(mediaBatch);
                postHashtagRepository.saveAll(postHashtagsBatch);

                mediaBatch.clear();
                postHashtagsBatch.clear();
                postsBatch.clear();
                log.info("Saved batch of {} posts", batchSize);
            }
        }

        log.info("Generating Comments & Reactions...");
        List<Post> allPosts = postRepository.findAll();
        List<Comment> commentsBatch = new ArrayList<>();
        List<Reaction> reactionsBatch = new ArrayList<>();
        List<SavedPost> savedPostsBatch = new ArrayList<>();
        List<Report> reportsBatch = new ArrayList<>();

        ReactionType[] rTypes = ReactionType.values();
        
        for (Post post : allPosts) {
            int reactionsCount = faker.number().numberBetween(0, 20);
            Set<Long> reactedUsers = new HashSet<>();
            for (int i = 0; i < reactionsCount; i++) {
                User rUser = users.get(faker.number().numberBetween(0, users.size()));
                if (reactedUsers.add(rUser.getId())) {
                    reactionsBatch.add(Reaction.builder()
                            .user(rUser)
                            .post(post)
                            .type(rTypes[faker.number().numberBetween(0, rTypes.length)])
                            .build());
                }
            }

            int commentsCount = faker.number().numberBetween(0, 5);
            for (int i = 0; i < commentsCount; i++) {
                User cUser = users.get(faker.number().numberBetween(0, users.size()));
                commentsBatch.add(Comment.builder()
                        .user(cUser)
                        .post(post)
                        .content(faker.lorem().sentence())
                        .build());
            }

            if (faker.number().numberBetween(1, 100) > 90) {
                User sUser = users.get(faker.number().numberBetween(0, users.size()));
                SavedPostId savedId = new SavedPostId(sUser.getId(), post.getId());
                savedPostsBatch.add(SavedPost.builder()
                        .id(savedId)
                        .user(sUser)
                        .post(post)
                        .build());
            }

            if (faker.number().numberBetween(1, 100) > 98) {
                User repUser = users.get(faker.number().numberBetween(0, users.size()));
                reportsBatch.add(Report.builder()
                        .reporter(repUser)
                        .post(post)
                        .reason(faker.lorem().sentence())
                        .status(ReportStatus.PENDING)
                        .build());
            }

            if (commentsBatch.size() >= 500) {
                reactionRepository.saveAll(reactionsBatch);
                commentRepository.saveAllAndFlush(commentsBatch);
                
                // Add Comment Reactions
                List<CommentReaction> commentReactions = new ArrayList<>();
                for (Comment c : commentsBatch) {
                    if (faker.bool().bool()) {
                        User crUser = users.get(faker.number().numberBetween(0, users.size()));
                        commentReactions.add(CommentReaction.builder()
                                .user(crUser)
                                .comment(c)
                                .type(rTypes[faker.number().numberBetween(0, rTypes.length)])
                                .build());
                    }
                }
                commentReactionRepository.saveAll(commentReactions);
                
                reactionsBatch.clear();
                commentsBatch.clear();
            }
        }
        
        if (!commentsBatch.isEmpty()) {
            reactionRepository.saveAll(reactionsBatch);
            commentRepository.saveAll(commentsBatch);
        }

        try { savedPostRepository.saveAll(savedPostsBatch); } catch (Exception e) {}
        try { reportRepository.saveAll(reportsBatch); } catch (Exception e) {}

        log.info("Post generation complete");
    }
}
