package media.social.modults.user.repository;

import media.social.modults.user.entity.Profile;
import media.social.modults.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUser(User user);

    Optional<Profile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}