package media.social.modules.dating.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modules.dating.dto.request.report.CreateDatingReportRequest;
import media.social.modules.dating.dto.request.report.UpdateDatingReportRequest;
import media.social.modules.dating.enums.DatingReportStatus;
import media.social.modules.dating.service.DatingReportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dating/reports")
@RequiredArgsConstructor
@Tag(
        name = "Dating Report Controller",
        description = "Dating report APIs"
)
public class DatingReportController {

    private final DatingReportService datingReportService;

    @PostMapping
    @Operation(summary = "Report a dating user")
    @RateLimit(
            name = "DATING_CREATE_REPORT",
            limit = 5,
            windowSeconds = 60
    )
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateDatingReportRequest request
    ) {
        datingReportService.create(request);

        return ResponseData.success(
                null,
                "Dating user reported successfully",
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/{reportId}/review")
    @Operation(summary = "Review a dating report")
    @RateLimit(
            name = "DATING_REVIEW_REPORT",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> review(
            @PathVariable Long reportId,
            @Valid @RequestBody UpdateDatingReportRequest request
    ) {
        datingReportService.review(
                reportId,
                request
        );

        return ResponseData.success(
                null,
                "Dating report reviewed successfully",
                HttpStatus.OK
        );
    }

    @GetMapping
    @Operation(summary = "Get all dating reports")
    @RateLimit(
            name = "DATING_GET_REPORTS",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> getAll(
            Pageable pageable
    ) {
        return ResponseData.success(
                datingReportService.getAll(pageable),
                "Get dating reports successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get dating reports by status")
    @RateLimit(
            name = "DATING_GET_REPORTS_BY_STATUS",
            limit = 60,
            windowSeconds = 60
    )
    public ResponseEntity<?> getByStatus(
            @PathVariable DatingReportStatus status,
            Pageable pageable
    ) {
        return ResponseData.success(
                datingReportService.getByStatus(
                        pageable,
                        status
                ),
                "Get dating reports by status successfully",
                HttpStatus.OK
        );
    }
}