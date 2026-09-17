package media.social.modules.post.service.domain;

import media.social.modules.post.entity.Post;

public interface PostDomainService {
    void validatePostExists (Long postId);

    Post getByPostId(Long postId);

    void checkOwner(Post post);

    void increaseCommentCount(Long postId);

    void decreaseCommentCount(Long postId);

    void increaseReactionCount(Long postId);

    void decreaseReactionCount(Long postId);
}
