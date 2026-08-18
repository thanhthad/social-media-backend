package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "dating_profile_interests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingProfileInterest {

    @EmbeddedId
    private DatingProfileInterestId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("datingProfileId")
    @JoinColumn(name = "dating_profile_id")
    private DatingProfile datingProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("interestId")
    @JoinColumn(name = "interest_id")
    private DatingInterest interest;

    private OffsetDateTime createdAt;
}