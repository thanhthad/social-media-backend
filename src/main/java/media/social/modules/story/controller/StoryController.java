package media.social.modules.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modules.story.dto.request.CreateStoryRequest;
import media.social.modules.story.dto.request.UpdateStoryVisibilityRequest;
import media.social.modules.story.service.StoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    // ================= CREATE STORY =================
    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Create a new story")
    public ResponseEntity<?> create(
            @ModelAttribute @Valid CreateStoryRequest request
    ) {

        storyService.create(request);

        return ResponseData.success(
                null,
                "Story created successfully",
                HttpStatus.CREATED
        );
    }

    // ================= GET STORY FEED =================
    @GetMapping("/feed")
    @Operation(summary = "Get story feed")
    public ResponseEntity<?> getFeed() {

        return ResponseData.success(
                storyService.getFeed(),
                "Get story feed successfully",
                HttpStatus.OK
        );
    }

    // ================= GET MY STORIES =================
    @GetMapping("/me")
    @Operation(summary = "Get my stories")
    public ResponseEntity<?> getMyStories() {

        return ResponseData.success(
                storyService.getMyStories(),
                "Get my stories successfully",
                HttpStatus.OK
        );
    }

    // ================= GET USER STORIES =================
    @GetMapping("/user/{targetUserId}")
    @Operation(summary = "Get user's stories")
    public ResponseEntity<?> getUserStories(
            @PathVariable Long targetUserId
    ) {

        return ResponseData.success(
                storyService.getUserStories(targetUserId),
                "Get user stories successfully",
                HttpStatus.OK
        );
    }

    // ================= GET USER STORIES BEFORE EXPIRE =================
    @GetMapping("/user/{targetUserId}/before-expire")
    @Operation(summary = "Get user's stories before expire")
    public ResponseEntity<?> getUserStoriesBeforeExpire(
            @PathVariable Long targetUserId
    ) {

        return ResponseData.success(
                storyService.getUserStoriesBeforeExpire(targetUserId),
                "Get user stories before expire successfully",
                HttpStatus.OK
        );
    }

    // ================= UPDATE VISIBILITY =================
    @PatchMapping("/{storyId}/visibility")
    @Operation(summary = "Update story visibility")
    public ResponseEntity<?> updateVisibility(
            @PathVariable Long storyId,
            @RequestBody @Valid UpdateStoryVisibilityRequest request
    ) {

        storyService.updateVisibility(
                storyId,
                request
        );

        return ResponseData.success(
                null,
                "Story visibility updated successfully",
                HttpStatus.OK
        );
    }

    // ================= DELETE STORY =================
    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete story")
    public ResponseEntity<?> delete(
            @PathVariable Long storyId
    ) {

        storyService.delete(storyId);

        return ResponseData.success(
                null,
                "Story deleted successfully",
                HttpStatus.OK
        );
    }
}