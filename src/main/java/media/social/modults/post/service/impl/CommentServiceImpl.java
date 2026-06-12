package media.social.modults.post.service.impl;

import lombok.AllArgsConstructor;
import media.social.modults.post.dto.request.CommentRequest;
import media.social.modults.post.dto.request.UpdateCommentContent;
import media.social.modults.post.dto.response.CommentResponse;
import media.social.modults.post.entity.Comment;
import media.social.modults.post.entity.Post;
import media.social.modults.post.exception.comment.CommentNotFoundException;
import media.social.modults.post.repository.CommentRepository;
import media.social.modults.post.service.CommentService;
import media.social.modults.post.service.PostServiceDomain;
import media.social.modults.user.entity.User;
import media.social.modults.user.security.context.UserContextHolder;
import media.social.modults.user.service.UserServiceDomain;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostServiceDomain postServiceDomain;
    private final UserServiceDomain userServiceDomain;

    @Override
    public CommentResponse createComment(CommentRequest request) {

        long userId = UserContextHolder.getUserId();

        Post post = postServiceDomain.getByPostId(request.getPostId());
        User user = userServiceDomain.getByUserId(userId);

        Comment parent = null;

        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() ->
                            new CommentNotFoundException("Parent comment not found")
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
                .build();
    }

    @Override
    public CommentResponse replyComment(CommentRequest request) {

        long userId = UserContextHolder.getUserId();

        Post post = postServiceDomain.getByPostId(request.getPostId());
        User user = userServiceDomain.getByUserId(userId);

        Comment parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() ->
                        new CommentNotFoundException("Parent comment not found")
                );

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
                .build();
    }

    @Override
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
                .build();
    }

    @Override
    public void deleteComment(Long commentId) {

        long userId = UserContextHolder.getUserId();

        Comment comment = commentRepository.findByIdAndUser_Id(commentId, userId)
                .orElseThrow(() ->
                        new CommentNotFoundException("Not found or not allowed")
                );

        commentRepository.delete(comment);
    }

    @Override
    public Page<CommentResponse> getRootComments(Long postId, Pageable pageable) {
        return commentRepository.findRootComments(postId, pageable);
    }

    @Override
    public Page<CommentResponse> getReplies(Long parentId, Pageable pageable) {
        return commentRepository.findReplies(parentId, pageable);
    }

    @Override
    public long countCommentsByPost(Long postId) {
        return commentRepository.countByPost_Id(postId);
    }
}