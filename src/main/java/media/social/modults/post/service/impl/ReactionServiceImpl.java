package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.notification.service.NotificationService;
import media.social.modults.post.dto.response.reaction.ReactionCountResponse;
import media.social.modults.post.dto.response.reaction.ReactionResponse;
import media.social.modults.post.dto.response.reaction.UserReactionResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.Reaction;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.exception.reaction.ReactionNotFoundException;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.post.repository.ReactionRepository;
import media.social.modults.post.service.ReactionService;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostRepository postRepository;
    private final UserServiceDomain userServiceDomain;
    private final NotificationService notificationService;

    private Post getPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("Post Not Found"));
    }

    @Override
    @Transactional
    public void react(Long postId, ReactionType type) {

        Long userId = UserContextHolder.getUserId();

        Post post = getPost(postId);

        User user = userServiceDomain.getByUserId(userId);

        Reaction reaction = reactionRepository
                .findByUserIdAndPostId(userId, postId)
                .orElse(null);

        if (reaction == null) {

            reaction = Reaction.builder()
                    .user(user)
                    .post(post)
                    .type(type)
                    .build();

            reactionRepository.save(reaction);
            notificationService.create(
                    post.getUser(),
                    user,
                    EntityType.POST,
                    post.getId(),
                    NotificationType.POST_REACTION
            );
            return;
        }

        if (reaction.getType() != type) {
            reaction.setType(type);
            reactionRepository.save(reaction);
        }
    }

    @Override
    @Transactional
    public void removeReaction(Long postId) {

        Long userId = UserContextHolder.getUserId();

        Post post = getPost(postId);

        Reaction reaction = reactionRepository
                .findByUserIdAndPostId(userId, postId)
                .orElseThrow(
                        () -> new ReactionNotFoundException("Reaction Not Found")
                );

        reactionRepository.delete(reaction);

        notificationService.delete(
                post.getUser().getId(),
                userId,
                EntityType.POST,
                post.getId(),
                NotificationType.POST_REACTION
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionResponse getMyReaction(Long postId) {

        Long userId = UserContextHolder.getUserId();

        getPost(postId);

        Reaction reaction = reactionRepository
                .findByUserIdAndPostId(userId, postId)
                .orElse(null);

        if (reaction == null) {

            return ReactionResponse.builder()
                    .reacted(false)
                    .type(null)
                    .build();
        }

        return ReactionResponse.builder()
                .reacted(true)
                .type(reaction.getType())
                .build();
    }

    @Override
    public long getTotalReaction(Long postId) {
        return reactionRepository.countByPostId(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionCountResponse countReaction(Long postId) {

        getPost(postId);

        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);

        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }

        List<Object[]> results =
                reactionRepository.countReactionTypesByPostId(postId);

        for (Object[] row : results) {

            ReactionType type = (ReactionType) row[0];

            Long count = (Long) row[1];

            counts.put(type, count);
        }

        return ReactionCountResponse.builder()
                .counts(counts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserReactionResponse> getUsersReacted(
            Long postId,
            ReactionType type,
            Pageable pageable
    ) {

        getPost(postId);

        return reactionRepository.findUsersReacted(
                postId,
                type,
                pageable
        );
    }
}