package media.social.modults.post.controller;


import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.post.service.HashtagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
public class HashtagController {

    private final HashtagService hashtagService;


    // ================= SEARCH HASHTAG =================
    @GetMapping("/search")
    @Operation(summary = "Search hashtags by keyword")
    public ResponseEntity<?> searchHashtags(
            @RequestParam String keyword
    ) {

        return ResponseData.success(
                hashtagService.search(keyword),
                "Search hashtags successfully",
                HttpStatus.OK
        );
    }


    // ================= TRENDING HASHTAGS =================
    @GetMapping("/trending")
    @Operation(summary = "Get trending hashtags")
    public ResponseEntity<?> getTrendingHashtags() {

        return ResponseData.success(
                hashtagService.getTrending(),
                "Get trending hashtags successfully",
                HttpStatus.OK
        );
    }

}