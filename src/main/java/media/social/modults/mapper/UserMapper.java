package media.social.modults.mapper;

import media.social.modults.dto.request.UserRequest;
import media.social.modults.dto.response.UserResponse;
import media.social.modults.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Entity → Response (auto map hết vì trùng field)
    UserResponse toResponse(User user);

    // Request → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    User toEntity(UserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromRequest(UserRequest request, @MappingTarget User user);
}