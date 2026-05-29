package media.social.modults.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import media.social.common.response.ResponseData;
import media.social.modults.dto.request.UserRequest;
import media.social.modults.dto.response.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "APIs for User management")
public class UserController {

    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create user")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseData.success(response, "Create user successfully", HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UserRequest request) {
        UserResponse response = userService.updateById(id, request);
        return ResponseData.success(response, "Update user successfully", HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user by Id")
    public ResponseEntity<?> deleteUserById(@PathVariable Long id) {
        userService.deleteById(id);
        return ResponseData.success(null, "Delete user successfully", HttpStatus.OK);
    }

    @DeleteMapping("/{email}")
    @Operation(summary = "Delete user by Email")
    public ResponseEntity<?> deleteUserByEmail(@PathVariable String email) {
        userService.deleteByEmail(email);
        return ResponseData.success(null, "Delete user successfully", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getById(id);
        return ResponseData.success(response, "Get user successfully", HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get all users with pagination")
    public ResponseEntity<?> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userService.getAll(pageable);
        return ResponseData.successPaginate(page, "Get all users successfully", HttpStatus.OK);
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username")
    public ResponseEntity<?> searchByUsername(@RequestParam String username,
                                              Pageable pageable) {
        Page<UserResponse> page = userService.getByUsername(username, pageable);
        return ResponseData.successPaginate(page, "Search users successfully", HttpStatus.OK);
    }

    @GetMapping("/email")
    @Operation(summary = "Get user by email")
    public ResponseEntity<?> getByEmail(@RequestParam String email) {
        UserResponse response = userService.getByEmail(email);
        return ResponseData.success(response, "Get user by email successfully", HttpStatus.OK);
    }

    @PutMapping("/{id}/avatar")
    @Operation(summary = "Upload user avatar")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {

        UserResponse response = userService.uploadAvatar(id, file);

        return ResponseData.success(
                response,
                "Upload avatar successfully",
                HttpStatus.OK
        );
    }

}