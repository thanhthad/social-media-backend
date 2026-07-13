package media.social.modults.post.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.service.SavedPostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/saved-posts")
@RequiredArgsConstructor
public class SavedPostController {

    private final SavedPostService savedPostService;

    @PostMapping("/{postId}")
    @Operation(summary = "Save a post")
    public ResponseEntity<?> savePost(
            @PathVariable Long postId
    ) {

        savedPostService.savePost(postId);

        return ResponseData.success(
                null,
                "Save post successfully",
                HttpStatus.CREATED
        );
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Unsave a post")
    public ResponseEntity<?> unsavePost(
            @PathVariable Long postId
    ) {

        savedPostService.unsavePost(postId);

        return ResponseData.success(
                null,
                "Unsave post successfully",
                HttpStatus.OK
        );
    }

    @GetMapping("/{postId}/status")
    @Operation(summary = "Check saved status")
    public ResponseEntity<?> isSaved(
            @PathVariable Long postId
    ) {

        return ResponseData.success(
                savedPostService.isSaved(postId),
                "Get saved status successfully",
                HttpStatus.OK
        );
    }
}