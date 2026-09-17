package media.social.modules.reel.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateReelViewRequest {

    @NotNull(message = "watchDurationMs is required")
    @Min(value = 0, message = "watchDurationMs must be >= 0")
    private Long watchDurationMs;

    @NotNull(message = "completed is required")
    private Boolean completed;
}
