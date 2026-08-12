package media.social.modules.user.service.domain;

public interface FriendShipDomain {

    boolean areFriends(Long userId, Long targetUserId);
}
