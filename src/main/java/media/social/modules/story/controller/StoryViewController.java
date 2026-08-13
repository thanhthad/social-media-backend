package media.social.modules.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modules.story.service.StoryViewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryViewController {

    private final StoryViewService storyViewService;

    // ================= VIEW STORY =================
    @PostMapping("/{storyId}/view")
    @Operation(summary = "View story")
    public ResponseEntity<?> view(
            @PathVariable Long storyId
    ) {

        storyViewService.view(storyId);

        return ResponseData.success(
                null,
                "Story viewed successfully",
                HttpStatus.OK
        );
    }
}