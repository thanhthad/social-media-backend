package media.social.modults.others.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(
            min = 3,
            max = 50,
            message = "Username must be between 3 and 50 characters"
    )
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(
            max = 100,
            message = "Email must not exceed 100 characters"
    )
    private String email;
}