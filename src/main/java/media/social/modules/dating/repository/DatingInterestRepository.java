package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingInterest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatingInterestRepository extends JpaRepository<DatingInterest, Long> {

    List<DatingInterest> findAllByIdIn(List<Long> ids);
}