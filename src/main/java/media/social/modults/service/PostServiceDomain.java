package media.social.modults.service;

import media.social.modults.entity.Post;

public interface PostServiceDomain {
    void validatePostExists (Long postId);

    Post getByPostId(Long postId);

}
