package media.social.modules.dating.service;

import media.social.modules.dating.dto.response.discovery.DatingDiscoveryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatingDiscoveryService {

    Page<DatingDiscoveryResponse> findDiscovery(Pageable pageable);
}
