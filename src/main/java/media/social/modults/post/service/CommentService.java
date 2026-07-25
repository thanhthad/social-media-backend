package media.social.modults.post.service;

import media.social.modults.post.dto.request.comment.CreateCommentRequest;
import media.social.modults.post.dto.request.comment.ReplyCommentRequest;
import media.social.modults.post.dto.request.comment.UpdateCommentContent;
import media.social.modults.post.dto.response.comment.CommentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommentService {

    CommentResponse createComment(CreateCommentRequest request);

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

}