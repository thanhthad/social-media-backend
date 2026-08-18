package media.social.modules.dating.service;

import media.social.modules.dating.dto.request.preference.UpdateDatingPreferenceRequest;
import media.social.modules.dating.dto.response.preference.DatingPreferenceResponse;

public interface DatingPreferenceService {

    DatingPreferenceResponse createPreference(UpdateDatingPreferenceRequest request);

    DatingPreferenceResponse getMyPreference();

    DatingPreferenceResponse updatePreference(UpdateDatingPreferenceRequest request);
}