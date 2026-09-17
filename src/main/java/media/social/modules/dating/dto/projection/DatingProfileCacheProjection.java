package media.social.modules.dating.dto.projection;

import media.social.modules.post.enums.Visibility;
import media.social.modules.user.enums.Gender;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface DatingProfileCacheProjection {

    String getUsername();

    String getAvatarUrl();

    String getCoverUrl();

    String getDisplayName();

    String getBio();

    Gender getGender();

    LocalDate getBirthday();

    Integer getHeight();

    String getOccupation();

    String getEducation();

    String getCountry();

    String getCity();

    String getDistrict();

    Boolean getActive();

    Visibility getVisibility();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}