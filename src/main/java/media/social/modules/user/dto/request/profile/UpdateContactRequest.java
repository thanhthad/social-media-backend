package media.social.modules.user.dto.request.profile;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class UpdateContactRequest {

    @Pattern(
            regexp = "^(03|05|07|08|09)[0-9]{8}$",
            message = "Invalid phone number"
    )
    private String phone;


    @Size(
            max = 255,
            message = "Website cannot exceed 255 characters"
    )
    private String website;


    @Size(
            max = 100,
            message = "Country cannot exceed 100 characters"
    )
    private String country;


    @Size(
            max = 100,
            message = "City cannot exceed 100 characters"
    )
    private String city;


    @Size(
            max = 100,
            message = "District cannot exceed 100 characters"
    )
    private String district;
}