package media.social.modules.reel.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReelViewResponse {

    private Long viewId;

    private Long reelId;

    private Long userId;

    private Long watchDurationMs;

    private Boolean completed;

    private Integer replayCount;

    private OffsetDateTime createdAt;
}
