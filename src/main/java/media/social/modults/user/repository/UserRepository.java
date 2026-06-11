package media.social.modults.user.repository;

import media.social.modults.user.Enum.Status;
import media.social.modults.user.dto.response.pub.UserSearchResponse;
import media.social.modults.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Query("""
SELECT new media.social.modults.user.dto.response.pub.UserSearchResponse(
    u.id,
    u.username,
    p.avatarUrl,
    p.fullName
)
FROM User u
JOIN u.profile p
WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :username, '%'))
AND u.status = :status
""")
    Page<UserSearchResponse> searchUsers(
            @Param("username") String username,
            @Param("status") Status status,
            Pageable pageable
    );
}