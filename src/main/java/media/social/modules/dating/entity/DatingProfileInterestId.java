package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DatingProfileInterestId implements Serializable {

    private Long datingProfileId;

    private Long interestId;
}