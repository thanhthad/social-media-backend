package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.service.DatingDiscoveryService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dating/discovery")
@RequiredArgsConstructor
@Tag(
        name = "Dating Discovery Controller",
        description = "Dating discovery APIs"
)
public class DatingDiscoveryController {

    private final DatingDiscoveryService datingDiscoveryService;

    @GetMapping
    @Operation(summary = "Get dating discovery candidates")
    @RateLimit(
            name = "DATING_DISCOVERY",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> findDiscovery(
            Pageable pageable
    ) {
        return ResponseData.success(
                datingDiscoveryService.findDiscovery(pageable),
                "Get dating discovery successfully",
                HttpStatus.OK
        );
    }
}
