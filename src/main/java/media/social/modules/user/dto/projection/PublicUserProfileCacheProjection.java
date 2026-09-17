package media.social.modules.user.dto.projection;

import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

public interface PublicUserProfileCacheProjection {

    Long getId();

    String getEmail();

    String getUsername();

    String getAvatarUrl();

    String getCoverUrl();

    String getBio();

    String getFullName();

    String getWebsite();

    String getPhone();

    LocalDate getDateOfBirth();

    Gender getGender();

    String getCountry();

    String getCity();

    String getDistrict();

    String getOccupation();

    String getCompany();

    String getEducation();

    Visibility getVisibility();

    Map<String, String> getSocialLinks();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}