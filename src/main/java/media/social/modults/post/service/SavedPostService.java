package media.social.modults.post.service;

import media.social.modults.post.entity.SavedPost;

import java.util.List;

public interface SavedPostService {

    void savePost(Long userId, Long postId);

    void unsavePost(Long userId, Long postId);

    boolean isSaved(Long userId, Long postId);

}