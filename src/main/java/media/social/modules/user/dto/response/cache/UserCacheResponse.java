package media.social.modules.user.dto.response.cache;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheResponse implements Serializable {

    private Long id;

    private String username;

    private String fullname;

    private String avatarUrl;

}