package media.social.modules.dating.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "dating_interests"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatingInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            unique = true,
            nullable = false,
            length = 100
    )
    private String name;
}