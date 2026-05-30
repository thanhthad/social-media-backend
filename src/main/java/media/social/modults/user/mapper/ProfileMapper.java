package media.social.modults.user.mapper;

import media.social.modults.user.dto.request.self.UpdateProfileRequest;
import media.social.modults.user.dto.response.self.ProfileResponse;
import media.social.modults.user.entity.Profile;
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