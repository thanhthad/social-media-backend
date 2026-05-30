package media.social.modults.others.mapper;

import media.social.modults.others.dto.request.UpdatePostRequest;
import media.social.modults.others.dto.response.PostResponse;
import media.social.modults.others.entity.Post;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PostMapper {

    // Entity → Response (map nested User → flat response)
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.profile.avatarUrl", target = "avatarUrl")
    PostResponse toResponse(Post post);

    // Request → Entity (user set ở service)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Post toEntity(UpdatePostRequest request);
}