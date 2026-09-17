package media.social.modules.conversation.dto.projection;

public interface UnreadCountProjection {

    Long getConversationId();

    Long getUnreadCount();
}