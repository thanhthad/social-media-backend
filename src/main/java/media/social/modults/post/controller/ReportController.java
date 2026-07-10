package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.dto.request.report.CreateReportRequest;
import media.social.modults.post.dto.request.report.UpdateReportRequest;
import media.social.modults.post.enums.ReportStatus;
import media.social.modults.post.service.ReportService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // ================= CREATE REPORT =================

    @PostMapping
    @Operation(summary = "Report a post")
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

    @PatchMapping("/{reportId}")
    @Operation(summary = "Review report")
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

    @GetMapping
    @Operation(summary = "Get all reports")
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

    @GetMapping("/status")
    @Operation(summary = "Get reports by status")
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