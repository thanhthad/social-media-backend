package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingProfileInterest;
import media.social.modules.dating.entity.DatingProfileInterestId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatingProfileInterestRepository
        extends JpaRepository<DatingProfileInterest, DatingProfileInterestId> {

    List<DatingProfileInterest> findByDatingProfileId(Long datingProfileId);

    void deleteByDatingProfileId(Long datingProfileId);
}