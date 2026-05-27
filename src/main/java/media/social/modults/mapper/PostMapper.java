package media.social.modults.mapper;

import media.social.modults.dto.request.CreatePostRequest;
import media.social.modults.dto.request.UpdatePostRequest;
import media.social.modults.dto.response.PostResponse;
import media.social.modults.entity.Post;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PostMapper {

    // Entity → Response (map nested User → flat response)
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.avatarUrl", target = "avatarUrl")
    PostResponse toResponse(Post post);

    // Request → Entity (user set ở service)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Post toEntity(UpdatePostRequest request);
}