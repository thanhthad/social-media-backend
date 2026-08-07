package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.interest.UpdateDatingInterestRequest;
import media.social.modules.dating.dto.response.interest.DatingInterestResponse;

import java.util.List;

public interface DatingInterestService {

    List<DatingInterestResponse> getAllInterest();

    List<DatingInterestResponse> getMyInterest();

    List<DatingInterestResponse> updateMyInterest(UpdateDatingInterestRequest request);
}