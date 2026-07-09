package media.social.modults.post.repository;

import media.social.modults.post.entity.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByName(String name);

    boolean existsByName(String name);

    List<Hashtag> findByNameStartingWithIgnoreCase(String keyword);

}
