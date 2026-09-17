package media.social.modules.user.dto.request.profile;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateBasicProfileRequest {

    @Size(
            max = 100,
            message = "Full name cannot exceed 100 characters"
    )
    private String fullName;

    @Size(
            max = 500,
            message = "Bio cannot exceed 500 characters"
    )
    private String bio;

    @Past(
            message = "Date of birth must be in the past"
    )
    private LocalDate dateOfBirth;

    private Gender gender;
}