package media.social.modults.post.dto.request.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReportRequest {

    @NotNull
    private Long postId;

    @NotBlank
    private String reason;

}