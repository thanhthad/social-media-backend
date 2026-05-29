package media.social.modults.mapper;

import media.social.modults.dto.request.LikeRequest;
import media.social.modults.dto.response.LikeResponse;
import media.social.modults.entity.Like;
import media.social.modults.entity.Post;
import media.social.modults.user.entity.User
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LikeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Like toEntity(LikeRequest request, User user, Post post);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "post.id", target = "postId")
    LikeResponse toResponse(Like like);
}