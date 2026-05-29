package media.social.modults.user.dto.response.self;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ProfileResponse {

    private Long id;

    private String fullName;

    private String avatarUrl;

    private String bio;

    private String phone;

    private LocalDate dateOfBirth;

    private String gender;

    private String location;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}