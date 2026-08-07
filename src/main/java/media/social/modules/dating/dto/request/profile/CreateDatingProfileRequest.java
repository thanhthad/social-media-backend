package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.*;
import lombok.Data;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;

@Data
public class CreateDatingProfileRequest {

    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must be less than 100 characters")
    private String displayName;

    @Size(max = 500, message = "Bio must be less than 500 characters")
    private String bio;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Birthday is required")
    @Past(message = "Birthday must be in the past")
    private LocalDate birthday;

    @NotNull(message = "Height is required")
    @Min(value = 100, message = "Height must be at least 100 cm")
    @Max(value = 250, message = "Height must be less than 250 cm")
    private Integer height;

    @Size(max = 100, message = "Occupation must be less than 100 characters")
    private String occupation;

    @Size(max = 150, message = "Education must be less than 150 characters")
    private String education;

    @Size(max = 100, message = "Country must be less than 100 characters")
    private String country;

    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;

    @Size(max = 100, message = "District must be less than 100 characters")
    private String district;
}