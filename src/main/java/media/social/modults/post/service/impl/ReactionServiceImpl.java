package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.notification.service.NotificationService;
import media.social.modults.post.dto.response.reaction.ReactionCountResponse;
import media.social.modults.post.dto.response.reaction.UserReactionResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.Reaction;
import media.social.modults.post.enums.ReactionType;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.post.exception.reaction.ReactionNotFoundException;
import media.social.modults.post.repository.PostRepository;
import media.social.modults.post.repository.ReactionRepository;
import media.social.modults.post.service.ReactionService;
import media.social.modults.post.service.domain.PostDomainService;
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
    private final PostDomainService postDomainService;
    private final UserServiceDomain userServiceDomain;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void react(Long postId, ReactionType type) {

        Long userId = UserContextHolder.getUserId();

        Post post = postDomainService.getByPostId(postId);

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

            postDomainService.increaseReactionCount(postId);

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

        Post post = postDomainService.getByPostId(postId);

        Reaction reaction = reactionRepository
                .findByUserIdAndPostId(userId, postId)
                .orElseThrow(
                        () -> new ReactionNotFoundException("Reaction Not Found")
                );

        reactionRepository.delete(reaction);
        postDomainService.decreaseReactionCount(postId);
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
    public ReactionCountResponse countReaction(Long postId) {

        postDomainService.getByPostId(postId);

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

        postDomainService.getByPostId(postId);

        return reactionRepository.findUsersReacted(
                postId,
                type,
                pageable
        );
    }
}