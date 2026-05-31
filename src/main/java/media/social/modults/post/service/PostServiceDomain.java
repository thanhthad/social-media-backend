package media.social.modults.post.service;

import media.social.modults.post.entity.Post;

public interface PostServiceDomain {
    void validatePostExists (Long postId);

    Post getByPostId(Long postId);

}
