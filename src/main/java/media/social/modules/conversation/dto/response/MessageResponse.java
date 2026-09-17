package media.social.modules.conversation.dto.response;

import lombok.Builder;
import lombok.Getter;
import media.social.modules.post.enums.ReactionType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MessageResponse {

    private Long id;

    private Long senderId;

    private String senderName;

    private String avatarUrl;

    private String content;

    private Long replyToMessageId;

    private List<MessageMediaResponse> medias;

    private Long totalReactions;

    private Map<ReactionType, Long> counts;

    private ReactionType myReaction;

    private OffsetDateTime createdAt;

}