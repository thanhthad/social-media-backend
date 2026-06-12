package media.social.modults.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCommentRequest {

    @NotNull(message = "Post id không được để trống")
    private Long postId;

    private Long parentId;

    @NotBlank(message = "Nội dung comment không được để trống")
    @Size(
            min = 1,
            max = 1000,
            message = "Nội dung comment phải từ 1 đến 1000 ký tự"
    )
    private String content;

}