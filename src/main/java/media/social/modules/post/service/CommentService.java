package media.social.modules.post.service;

import media.social.modules.post.dto.request.comment.CreateCommentRequest;
import media.social.modules.post.dto.request.comment.UpdateCommentContent;
import media.social.modules.post.dto.response.comment.CommentResponse;
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