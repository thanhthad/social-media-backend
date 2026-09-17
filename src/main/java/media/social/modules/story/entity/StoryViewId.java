package media.social.modules.story.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class StoryViewId implements Serializable {

    private Long storyId;

    private Long viewerId;
}