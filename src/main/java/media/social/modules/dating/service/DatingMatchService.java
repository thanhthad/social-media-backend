package media.social.modules.dating.service;

import media.social.modules.dating.dto.response.match.DatingMatchResponse;
import media.social.modules.user.entity.User;

import java.util.List;

public interface DatingMatchService {

    void createMatch(User userOne, User userTwo);

    List<DatingMatchResponse> getMyMatches();

    void unmatch(Long matchId);
}
