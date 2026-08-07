package media.social.modules.dating.dto.request.profile;

import jakarta.validation.constraints.*;
import lombok.Data;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;

@Data
public class UpdateDatingBasicInfoRequest {

    @Size(max = 100, message = "Display name must be less than 100 characters")
    private String displayName;

    private Gender gender;

    @Past(message = "Birthday must be in the past")
    private LocalDate birthday;

    @Min(value = 100, message = "Height must be at least 100 cm")
    @Max(value = 250, message = "Height must be less than 250 cm")
    private Integer height;
}