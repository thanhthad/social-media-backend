package media.social.modules.user.mapper;

import media.social.modules.user.dto.request.user.UpdateProfileRequest;
import media.social.modules.user.dto.response.user.ProfileResponse;
import media.social.modules.user.entity.Profile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProfileMapper {

    ProfileResponse toProfileResponse(Profile profile);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    void updateProfileFromRequest(
            UpdateProfileRequest request,
            @MappingTarget Profile profile
    );
}