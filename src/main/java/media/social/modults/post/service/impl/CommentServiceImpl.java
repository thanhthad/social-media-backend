package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.notification.enums.EntityType;
import media.social.modults.notification.enums.NotificationType;
import media.social.modults.notification.service.NotificationService;
import media.social.modults.post.dto.request.comment.CreateCommentRequest;
import media.social.modults.post.dto.request.comment.ReplyCommentRequest;
import media.social.modults.post.dto.request.comment.UpdateCommentContent;
import media.social.modults.post.dto.response.comment.CommentResponse;
import media.social.modults.post.entity.Comment;
import media.social.modults.post.entity.Post;
import media.social.modults.post.exception.comment.CommentNotFoundException;
import media.social.modults.post.repository.CommentReactionRepository;
import media.social.modults.post.repository.CommentRepository;
import media.social.modults.post.service.CommentService;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.domain.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostServiceDomain postServiceDomain;
    private final UserServiceDomain userServiceDomain;
    private final CommentReactionRepository commentReactionRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public CommentResponse createComment(CreateCommentRequest request) {

        long userId = UserContextHolder.getUserId();

        Post post = postServiceDomain.getByPostId(request.getPostId());

        User user = userServiceDomain.getByUserId(userId);

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() ->
                            new CommentNotFoundException(
                                    "Parent comment not found"
                            )
                    );
        }

        Comment saved = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .user(user)
                        .parent(parent)
                        .content(request.getContent())
                        .build()
        );
        if (parent != null) {
            notificationService.create(
                    parent.getUser(),
                    user,
                    EntityType.COMMENT,
                    parent.getId(),
                    NotificationType.COMMENT_REPLY
            );
        }
        else {
            notificationService.create(
                    post.getUser(),
                    user,
                    EntityType.POST,
                    post.getId(),
                    NotificationType.POST_COMMENT
            );
        }
        return CommentResponse.builder()
                .commentId(saved.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getProfile() != null
                        ? user.getProfile().getAvatarUrl()
                        : null)
                .parentId(parent != null ? parent.getId() : null)
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .totalReplies(0L)
                .totalReactions(0L)
                .myReaction(null)
                .build();
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Long userId = UserContextHolder.getUserId();

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new CommentNotFoundException("Comment not found")
                );

        boolean isCommentOwner =
                comment.getUser().getId().equals(userId);

        boolean isPostOwner =
                comment.getPost()
                        .getUser()
                        .getId()
                        .equals(userId);

        if (!isCommentOwner && !isPostOwner) {
            throw new AccessDeniedException(
                    "You don't have permission to delete this comment"
            );
        }

        notificationService.delete(
                comment.getPost().getUser().getId(),
                comment.getUser().getId(),
                EntityType.POST,
                comment.getPost().getId(),
                NotificationType.POST_COMMENT
        );

        commentRepository.delete(comment);
    }

    @Override
    @Transactional
    public CommentResponse replyComment(ReplyCommentRequest request) {

        long userId = UserContextHolder.getUserId();
        User user = userServiceDomain.getByUserId(userId);

        Comment parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() ->
                        new CommentNotFoundException("Parent comment not found")
                );
        Post post = postServiceDomain.getByPostId(parent.getPost().getId());

        Comment saved = commentRepository.save(
                Comment.builder()
                        .post(post)
                        .user(user)
                        .parent(parent)
                        .content(request.getContent())
                        .build()
        );

        return CommentResponse.builder()
                .commentId(saved.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .avatarUrl(user.getProfile() != null
                        ? user.getProfile().getAvatarUrl()
                        : null)
                .parentId(parent.getId())
                .content(saved.getContent())
                .createdAt(saved.getCreatedAt())
                .totalReplies(0L)
                .totalReactions(0L)
                .myReaction(null)
                .build();
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentContent request) {

        long userId = UserContextHolder.getUserId();

        Comment comment = commentRepository.findByIdAndUser_Id(commentId, userId)
                .orElseThrow(() ->
                        new CommentNotFoundException("Not found or not allowed")
                );

        comment.setContent(request.getContent());

        commentRepository.save(comment);

        return CommentResponse.builder()
                .commentId(comment.getId())
                .userId(userId)
                .username(comment.getUser().getUsername())
                .avatarUrl(comment.getUser().getProfile() != null
                        ? comment.getUser().getProfile().getAvatarUrl()
                        : null)
                .parentId(comment.getParent() != null
                        ? comment.getParent().getId()
                        : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .totalReplies(commentRepository.countByParent_Id(comment.getId()))
                .totalReactions(
                        commentReactionRepository.countByCommentId(comment.getId())
                )
                .myReaction(
                        commentReactionRepository.findReactionType(
                                userId,
                                comment.getId()
                        )
                )
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getRootComments(
            Long postId,
            Pageable pageable
    ) {

        Long userId = UserContextHolder.getUserId();

        return commentRepository.findRootComments(
                postId,
                userId,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommentResponse> getReplies(
            Long parentId,
            Pageable pageable
    ) {

        Long userId = UserContextHolder.getUserId();

        return commentRepository.findReplies(
                parentId,
                userId,
                pageable
        );
    }

    @Override
    @Transactional(readOnly = true)
    public long countCommentsByPost(Long postId) {
        return commentRepository.countByPost_Id(postId);
    }
}