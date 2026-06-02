package media.social.modults.post.mapper;

import media.social.modults.post.dto.request.UpdatePostContent;
import media.social.modults.post.dto.response.PostResponse;
import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.profile.avatarUrl", target = "avatarUrl")
    @Mapping(target = "imageUrl", expression = "java(mapImages(post))")
    PostResponse toResponse(Post post);

    default List<String> mapImages(Post post) {
        if (post.getMedia() == null) return new ArrayList<>();

        return post.getMedia()
                .stream()
                .map(PostMedia::getUrl)
                .toList();
    }
}