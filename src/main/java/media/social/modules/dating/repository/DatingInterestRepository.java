package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatingInterestRepository extends JpaRepository<DatingInterest, Long> {

    List<DatingInterest> findAllByIdIn(List<Long> ids);

    Optional<DatingInterest> findByName(String name);


}