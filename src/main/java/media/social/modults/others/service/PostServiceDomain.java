package media.social.modults.others.service;

import media.social.modults.others.entity.Post;

public interface PostServiceDomain {
    void validatePostExists (Long postId);

    Post getByPostId(Long postId);

}
