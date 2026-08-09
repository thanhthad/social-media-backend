package media.social.modules.dating.dto.response.discovery;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DatingDiscoveryResponse {

    private Long userId;

    private String displayName;

    private String avatarUrl;

    private Integer age;

    private String city;

    private Double distanceKm;

    private Integer compatibilityScore;
}