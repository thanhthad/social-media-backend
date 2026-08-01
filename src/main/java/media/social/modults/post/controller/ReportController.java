package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.ratelimit.annotation.RateLimit;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.report.CreateReportRequest;
import media.social.modults.post.dto.request.report.UpdateReportRequest;
import media.social.modults.post.enums.ReportStatus;
import media.social.modults.post.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ================= CREATE REPORT =================
    @PostMapping
    @Operation(summary = "Report a post")
    @RateLimit(
            name = "REPORT_CREATE",
            limit = 10,
            windowSeconds = 60
    )
    public ResponseEntity<?> createReport(
            @RequestBody @Valid CreateReportRequest request
    ) {

        reportService.create(request);

        return ResponseData.success(
                null,
                "Report created successfully",
                HttpStatus.CREATED
        );
    }

    // ================= REVIEW REPORT =================
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @PatchMapping("/{reportId}")
    @Operation(summary = "Review report")
    @RateLimit(
            name = "REPORT_REVIEW",
            limit = 30,
            windowSeconds = 60
    )
    public ResponseEntity<?> reviewReport(
            @PathVariable Long reportId,
            @RequestBody @Valid UpdateReportRequest request
    ) {

        reportService.review(
                reportId,
                request
        );

        return ResponseData.success(
                null,
                "Review report successfully",
                HttpStatus.OK
        );
    }

    // ================= GET ALL REPORTS =================
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @GetMapping
    @Operation(summary = "Get all reports")
    @RateLimit(
            name = "REPORT_GET_ALL",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getAll(
            Pageable pageable
    ) {

        return ResponseData.success(
                reportService.getAll(pageable),
                "Get all reports successfully",
                HttpStatus.OK
        );
    }

    // ================= GET REPORTS BY STATUS =================
    @PreAuthorize("hasAnyRole('ADMIN','MODERATOR')")
    @GetMapping("/status")
    @Operation(summary = "Get reports by status")
    @RateLimit(
            name = "REPORT_GET_BY_STATUS",
            limit = 120,
            windowSeconds = 60
    )
    public ResponseEntity<?> getByStatus(
            @RequestParam ReportStatus status,
            Pageable pageable
    ) {

        return ResponseData.success(
                reportService.getByStatus(
                        pageable,
                        status
                ),
                "Get reports successfully",
                HttpStatus.OK
        );
    }
}