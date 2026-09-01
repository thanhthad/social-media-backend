package media.social.modules.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.dto.response.post.PostReactionResponse;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.entity.Post;
import media.social.modules.post.entity.Reaction;
import media.social.modules.post.enums.PostType;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.exception.post.InvalidPostException;
import media.social.modules.post.exception.reaction.ReactionNotFoundException;
import media.social.modules.post.repository.ReactionRepository;
import media.social.modules.post.service.ReactionService;
import media.social.modules.post.service.domain.PostDomainService;
import media.social.modules.user.entity.User;
import media.social.modules.auth.security.context.UserContextHolder;
import media.social.modules.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionRepository reactionRepository;
    private final PostDomainService postDomainService;
    private final UserServiceDomain userServiceDomain;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public PostReactionResponse react(Long postId, ReactionType type) {

        Long userId = UserContextHolder.getUserId();

        Post post = postDomainService.getByPostId(postId);
        User user = userServiceDomain.getByUserId(userId);

        Reaction reaction = reactionRepository
                .findByUserIdAndPostId(userId, postId)
                .orElse(null);

        boolean isReacted;
        ReactionType reactionType;

        if (reaction == null) {

            reaction = Reaction.builder()
                    .user(user)
                    .post(post)
                    .type(type)
                    .build();

            reactionRepository.save(reaction);

            postDomainService.increaseReactionCount(postId);
            post.setReactionCount(post.getReactionCount() + 1);

            if (!post.getUser().getId().equals(userId)) {
                notificationService.create(
                        post.getUser(),
                        user,
                        EntityType.POST,
                        post.getId(),
                        NotificationType.POST_REACTION
                );
            }

            isReacted = true;
            reactionType = type;

        } else {

            if (reaction.getType() == type) {

                reactionRepository.delete(reaction);

                postDomainService.decreaseReactionCount(postId);
                post.setReactionCount(Math.max(0, post.getReactionCount() - 1));

                isReacted = false;
                reactionType = null;

            } else {

                reaction.setType(type);
                reactionRepository.save(reaction);

                isReacted = true;
                reactionType = type;
            }
        }

        PostReactionResponse response = new PostReactionResponse();

        response.setPostId(postId);
        response.setReacted(isReacted);
        response.setReactionType(reactionType);
        response.setReactionCount(post.getReactionCount());

        return response;
    }

    @Override
    @Transactional
    public PostReactionResponse loveReel(Long postId) {

        Long userId = UserContextHolder.getUserId();

        Post reel = postDomainService.getByPostId(postId);

        if (reel.getPostType() != PostType.REEL) {
            throw new InvalidPostException("Post is not a reel");
        }

        User user = userServiceDomain.getByUserId(userId);

        Reaction reaction = reactionRepository
                .findByUserIdAndPostId(userId, postId)
                .orElse(null);

        boolean isReacted;
        ReactionType reactionType;

        if (reaction == null) {

            reaction = Reaction.builder()
                    .user(user)
                    .post(reel)
                    .type(ReactionType.LOVE)
                    .build();

            reactionRepository.save(reaction);

            postDomainService.increaseReactionCount(postId);
            reel.setReactionCount(reel.getReactionCount() + 1);

            isReacted = true;
            reactionType = ReactionType.LOVE;

        } else {
            reactionRepository.delete(reaction);

            postDomainService.decreaseReactionCount(postId);
            reel.setReactionCount(Math.max(0, reel.getReactionCount() - 1));

            isReacted = false;
            reactionType = null;
        }

        PostReactionResponse response = new PostReactionResponse();

        response.setPostId(postId);
        response.setReacted(isReacted);
        response.setReactionType(reactionType);
        response.setReactionCount(reel.getReactionCount());

        return response;
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
        ).map(projection -> UserReactionResponse.builder()
                .id(projection.getId())
                .username(projection.getUserName())
                .fullName(projection.getFullName())
                .avatarUrl(projection.getAvatarUrl())
                .type(projection.getType())
                .createdAt(projection.getCreatedAt())
                .build());
    }
}