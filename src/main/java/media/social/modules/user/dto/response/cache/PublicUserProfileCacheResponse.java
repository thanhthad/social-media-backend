package media.social.modules.user.dto.response.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicUserProfileCacheResponse {

    private Long id;

    private String username;

    private String email;

    private String avatarUrl;

    private String bio;

    private String fullName;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}