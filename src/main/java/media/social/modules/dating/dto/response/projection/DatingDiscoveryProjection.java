package media.social.modules.dating.dto.response.projection;

public interface DatingDiscoveryProjection {

    Long getUserId();

    String getDisplayName();

    String getAvatarUrl();

    Integer getAge();

    String getCity();

    Double getDistanceKm();

    Long getCommonInterestCount();

}