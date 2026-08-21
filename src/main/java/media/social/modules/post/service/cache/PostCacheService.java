package media.social.modules.post.service.cache;

import media.social.modules.post.dto.response.post.PostCacheDTO;
import media.social.modules.post.enums.Visibility;

import java.util.List;

public interface PostCacheService {

    PostCacheDTO getPost(
            Long postId,
            List<Visibility> visibilities
    );

    void evictPost(Long postId);
}