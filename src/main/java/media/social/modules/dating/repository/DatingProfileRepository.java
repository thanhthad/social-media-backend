package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingProfile;
import media.social.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatingProfileRepository
        extends JpaRepository<DatingProfile, Long> {

    Optional<DatingProfile> findByUser(User user);

    Optional<DatingProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

}