package media.social.modults.user.service.domain;

public interface BlockPolicyService {

    boolean isBlocked(Long viewerId, Long targetId);
}