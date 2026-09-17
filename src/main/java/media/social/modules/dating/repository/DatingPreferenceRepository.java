package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DatingPreferenceRepository
        extends JpaRepository<DatingPreference, Long> {

    Optional<DatingPreference> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}