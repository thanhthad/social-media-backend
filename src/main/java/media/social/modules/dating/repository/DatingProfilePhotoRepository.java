package media.social.modules.dating.repository;

import media.social.modules.dating.entity.DatingProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DatingProfilePhotoRepository
        extends JpaRepository<DatingProfilePhoto, Long> {

    @Query("""
    SELECT p
    FROM DatingProfilePhoto p
    JOIN FETCH p.datingProfile dp
    JOIN FETCH dp.user u
    WHERE p.id = :photoId
      AND u.id = :userId
""")
    Optional<DatingProfilePhoto> findByIdAndUserId(
            @Param("photoId") Long photoId,
            @Param("userId") Long userId
    );

    Optional<DatingProfilePhoto> findTopByDatingProfileIdOrderByDisplayOrderDesc(
            Long datingProfileId
    );

    Optional<DatingProfilePhoto> findTopByDatingProfileIdOrderByDisplayOrderAsc(
            Long datingProfileId
    );

    long countByDatingProfileId(Long datingProfileId);

    @Modifying
    @Query("""
        UPDATE DatingProfilePhoto p
        SET p.primary = false
        WHERE p.datingProfile.id = :datingProfileId
          AND p.primary = true
    """)
    int clearPrimaryPhoto(
            @Param("datingProfileId") Long datingProfileId
    );

    @Modifying
    @Query("""
        UPDATE DatingProfilePhoto p
        SET p.displayOrder = p.displayOrder - 1
        WHERE p.datingProfile.id = :datingProfileId
          AND p.displayOrder > :deletedOrder
    """)
    int decreaseDisplayOrderAfterDelete(
            @Param("datingProfileId") Long datingProfileId,
            @Param("deletedOrder") Integer deletedOrder
    );
}