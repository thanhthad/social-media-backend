package media.social.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshTokenRequest {

    @NotBlank(message = "token is required")
    @Size(min = 1, max = 1000, message = "Content max 1000 characters")
    private String refreshToken;
}