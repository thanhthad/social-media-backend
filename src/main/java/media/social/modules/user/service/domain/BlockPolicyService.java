package media.social.modules.user.service.domain;

public interface BlockPolicyService {

    boolean isBlocked(Long viewerId, Long targetId);
}