package media.social.modults.post.repository;

import media.social.modults.post.entity.SavedPost;
import media.social.modults.post.entity.SavedPostId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedPostRepository extends JpaRepository<SavedPost, SavedPostId> {

    boolean existsByUser_IdAndPost_Id(Long userId, Long postId);

    void deleteByUser_IdAndPost_Id(Long userId, Long postId);

}