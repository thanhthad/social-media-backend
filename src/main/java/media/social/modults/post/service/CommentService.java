package media.social.modults.post.service;

import media.social.modults.post.dto.request.CommentRequest;
import media.social.modults.post.dto.request.UpdateCommentContent;
import media.social.modults.post.dto.response.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse createComment(CommentRequest request);

    CommentResponse replyComment(CommentRequest request);

    CommentResponse updateComment(Long commentId, UpdateCommentContent request);

    void deleteComment(Long commentId);

    Page<CommentResponse> getRootComments(
            Long postId,
            Pageable pageable
    );

    Page<CommentResponse> getReplies(
            Long parentId,
            Pageable pageable
    );

    long countCommentsByPost(Long postId);

}