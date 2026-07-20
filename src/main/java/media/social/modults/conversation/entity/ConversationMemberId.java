package media.social.modults.conversation.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ConversationMemberId implements Serializable {

    private Long conversation;

    private Long user;

}