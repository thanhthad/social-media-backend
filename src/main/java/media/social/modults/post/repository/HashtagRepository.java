package media.social.modults.post.repository;

import media.social.modults.post.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByName(String name);

    boolean existsByName(String name);

    List<Hashtag> findByNameStartingWithIgnoreCase(String keyword);

    @Modifying
    @Query("""
    DELETE FROM Hashtag h
    WHERE h.hashtagId = :id
    AND NOT EXISTS (
        SELECT ph.id
        FROM PostHashtag ph
        WHERE ph.hashtag.hashtagId = :id
    )
    """)
    void deleteIfUnused(Long id);

}
