package media.social.modults.others.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import media.social.modults.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "follows")
@IdClass(FollowId.class)
@Setter
@Getter
public class Follow {

    @Id
    @ManyToOne
    @JoinColumn(name = "follower_id")
    private User follower;

    @Id
    @ManyToOne
    @JoinColumn(name = "following_id")
    private User following;

    private LocalDateTime createdAt = LocalDateTime.now();
}