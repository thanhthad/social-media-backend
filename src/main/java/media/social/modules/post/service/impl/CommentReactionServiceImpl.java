package media.social.modules.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modules.notification.enums.EntityType;
import media.social.modules.notification.enums.NotificationType;
import media.social.modules.notification.service.NotificationService;
import media.social.modules.post.dto.response.reaction.ReactionCountResponse;
import media.social.modules.post.dto.response.reaction.ReactionResponse;
import media.social.modules.post.dto.response.reaction.UserReactionResponse;
import media.social.modules.post.entity.Comment;
import media.social.modules.post.entity.CommentReaction;
import media.social.modules.post.enums.ReactionType;
import media.social.modules.post.exception.comment.CanNotCommentYourself;
import media.social.modules.post.exception.comment.CommentNotFoundException;
import media.social.modules.post.exception.reaction.ReactionNotFoundException;
import media.social.modules.post.repository.CommentReactionRepository;
import media.social.modules.post.repository.CommentRepository;
import media.social.modules.post.service.CommentReactionService;
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

@Service
@AllArgsConstructor
public class CommentReactionServiceImpl
        implements CommentReactionService {

    private final CommentReactionRepository commentReactionRepository;
    private final CommentRepository commentRepository;
    private final UserServiceDomain userServiceDomain;
    private final NotificationService notificationService;

    private Comment getComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new CommentNotFoundException("Comment Not Found"));
    }

    @Override
    @Transactional
    public void react(
            Long commentId,
            ReactionType type
    ) {

        Long userId = UserContextHolder.getUserId();

        Comment comment = getComment(commentId);

        User user = userServiceDomain.getByUserId(userId);

        if(comment.getUser().getId().equals(userId)){
            throw new CanNotCommentYourself("Can not comment yourself");
        }
        CommentReaction reaction =
                commentReactionRepository
                        .findByUserIdAndCommentId(userId, commentId)
                        .orElse(null);

        if (reaction == null) {

            reaction = CommentReaction.builder()
                    .user(user)
                    .comment(comment)
                    .type(type)
                    .build();

            commentReactionRepository.save(reaction);

            notificationService.create(
                    comment.getUser(),
                    user,
                    EntityType.COMMENT,
                    comment.getId(),
                    NotificationType.COMMENT_REACTION
            );

            return;
        }

        if (reaction.getType() != type) {

            reaction.setType(type);

            commentReactionRepository.save(reaction);
        }
    }

    @Override
    @Transactional
    public void removeReaction(Long commentId) {

        Long userId = UserContextHolder.getUserId();

        Comment comment = getComment(commentId);

        CommentReaction reaction =
                commentReactionRepository
                        .findByUserIdAndCommentId(userId, commentId)
                        .orElseThrow(() ->
                                new ReactionNotFoundException(
                                        "Reaction Not Found"));

        commentReactionRepository.delete(reaction);

        notificationService.delete(
                comment.getUser().getId(),
                userId,
                EntityType.COMMENT,
                comment.getId(),
                NotificationType.COMMENT_REACTION
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ReactionResponse getMyReaction(Long commentId) {

        Long userId = UserContextHolder.getUserId();

        getComment(commentId);

        CommentReaction reaction =
                commentReactionRepository
                        .findByUserIdAndCommentId(userId, commentId)
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
    @Transactional(readOnly = true)
    public ReactionCountResponse countReaction(Long commentId) {

        getComment(commentId);

        Map<ReactionType, Long> counts =
                new EnumMap<>(ReactionType.class);

        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0L);
        }

        List<Object[]> results =
                commentReactionRepository
                        .countReactionsByCommentId(commentId);

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
            Long commentId,
            ReactionType type,
            Pageable pageable
    ) {

        getComment(commentId);

        return commentReactionRepository
                .findUsersReacted(
                        commentId,
                        type,
                        pageable
                )
                .map(projection ->
                        UserReactionResponse.builder()
                                .id(projection.getId())
                                .username(projection.getUserName())
                                .avatarUrl(projection.getAvatarUrl())
                                .createdAt(projection.getCreatedAt())
                                .build()
                );
    }
}