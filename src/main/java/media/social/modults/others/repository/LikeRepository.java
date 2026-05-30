package media.social.modults.others.repository;

import media.social.modults.others.entity.Like;
import media.social.modults.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LikeRepository extends JpaRepository<Like, Long> {

    /*
     =========================================================
     1. Unlike post
     =========================================================
     */
    void deleteByUserIdAndPostId(Long userId, Long postId);


    /*
     =========================================================
     2. Count likes of a post
     =========================================================
     */
    long countByPostId(Long postId);


    /*
     =========================================================
     3. Check user liked post
     =========================================================
     */
    boolean existsByUserIdAndPostId(Long userId, Long postId);


    /*
     =========================================================
     4. Find specific like
     =========================================================
     */
    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);


    /*
     =========================================================
     5. Users who liked post (PAGE)
     =========================================================
     */
    @Query("""
           SELECT l.user
           FROM Like l
           WHERE l.post.id = :postId
           """)
    Page<User> findUsersWhoLikedPost(@Param("postId") Long postId, Pageable pageable);


    /*
     =========================================================
     6. Likes by user (PAGE)
     =========================================================
     */
    Page<Like> findByUserId(Long userId, Pageable pageable);


    /*
     =========================================================
     7. Top liked posts (PAGE)
     =========================================================
     */
    @Query("""
           SELECT l.post, COUNT(l.id) as totalLikes
           FROM Like l
           GROUP BY l.post
           ORDER BY totalLikes DESC
           """)
    Page<Object[]> findTopLikedPosts(Pageable pageable);
}