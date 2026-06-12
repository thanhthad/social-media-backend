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

        Comment comment = null;
        Post post = postServiceDomain.getByPostId(request.getPostId());
        User user = userServiceDomain.getByUserId(userId);

        Comment saved =  commentRepository.save(Comment.builder()
                        .parent(comment)
                        .post(post)
                        .user(user)
                        .content(request.getContent())
                .build());
        return CommentResponse.builder()
                .commentId(saved.getId())
                .userId(userId)
                .username(user.getUsername())
                .avatarUrl(user.getProfile().getAvatarUrl())
                .parentId(null)
                .content(request.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public CommentResponse replyComment(CommentRequest request) {

        long userId = UserContextHolder.getUserId();

        Comment comment = null;
        Post post = postServiceDomain.getByPostId(request.getPostId());
        User user = userServiceDomain.getByUserId(userId);

        if (request.getParentId() != null) comment = commentRepository.findById(request.getParentId()).orElseThrow(
                () -> new CommentNotFoundException("Comment not found with parentId: " + request.getParentId())
        );

        Comment saved =  commentRepository.save(Comment.builder()
                .parent(comment)
                .post(post)
                .user(user)
                .content(request.getContent())
                .build());
        return CommentResponse.builder()
                .commentId(saved.getId())
                .userId(userId)
                .username(user.getUsername())
                .avatarUrl(user.getProfile().getAvatarUrl())
                .parentId(null)
                .content(request.getContent())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public CommentResponse updateComment(Long commentId, UpdateCommentContent request) {
        return null;
    }

    @Override
    public void deleteComment(Long commentId) {

    }

    @Override
    public Page<CommentResponse> getRootComments(Long postId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<CommentResponse> getReplies(Long parentId, Pageable pageable) {
        return null;
    }

    @Override
    public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
        return null;
    }

    @Override
    public long countCommentsByPost(Long postId) {
        return 0;
    }
}
