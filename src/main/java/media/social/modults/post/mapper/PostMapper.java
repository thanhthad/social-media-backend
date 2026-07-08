package media.social.modults.post.mapper;

import media.social.modults.post.entity.Post;
import media.social.modults.post.entity.PostMedia;
import org.mapstruct.*;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    default List<String> mapImages(Post post) {
        if (post.getMedia() == null) return new ArrayList<>();

        return post.getMedia()
                .stream()
                .map(PostMedia::getUrl)
                .toList();
    }
}