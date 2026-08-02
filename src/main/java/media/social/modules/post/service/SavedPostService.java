package media.social.modules.post.service;

public interface SavedPostService {

    void savePost(Long postId);

    void unsavePost(Long postId);

    boolean isSaved(Long postId);

}