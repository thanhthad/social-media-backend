package media.social.common.exception;

import lombok.extern.slf4j.Slf4j;
import media.social.common.response.ApiResponse;
import media.social.common.response.ResponseData;
import media.social.modults.file.image.exception.CloudinaryDeleteException;
import media.social.modults.file.image.exception.CloudinaryUploadException;
import media.social.modults.file.image.exception.InvalidImageException;
import media.social.modults.post.exception.comment.CommentAlreadyExistsException;
import media.social.modults.post.exception.comment.CommentNotFoundException;
import media.social.modults.post.exception.like.LikeAlreadyExistsException;
import media.social.modults.post.exception.like.LikeNotFoundException;
import media.social.modults.post.exception.post.InvalidDateRangeException;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.user.exception.follow.FollowAlreadyExistsException;
import media.social.modults.user.exception.follow.FollowNotFoundException;
import media.social.modults.user.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modults.user.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modults.user.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modults.user.exception.user.UserAlreadyExistsException;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.security.context.UserContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private Long getUserId(){
        return UserContextHolder.getUserId();
    }

    // ================= USER =================
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseData.fail("User not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseData.fail("User already exists", HttpStatus.CONFLICT);
    }

    // ================= POST =================
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostNotFound(PostNotFoundException ex) {
        return ResponseData.fail("Post not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        return ResponseData.fail("Invalid date range", HttpStatus.BAD_REQUEST);
    }

    // ================= LIKE =================
    @ExceptionHandler(LikeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleLikeAlreadyExists(LikeAlreadyExistsException ex) {
        return ResponseData.fail("Already liked", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LikeNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleLikeNotFound(LikeNotFoundException ex) {
        return ResponseData.fail("Like not found", HttpStatus.NOT_FOUND);
    }

    // ================= FOLLOW =================
    @ExceptionHandler(FollowAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleFollowAlreadyExists(FollowAlreadyExistsException ex) {
        return ResponseData.fail("Already following this user", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FollowNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleFollowNotFound(FollowNotFoundException ex) {
        return ResponseData.fail("Follow relationship not found", HttpStatus.NOT_FOUND);
    }

    // ================= COMMENT =================
    @ExceptionHandler(CommentAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommentAlreadyExists(CommentAlreadyExistsException ex) {
        return ResponseData.fail("Already comment this user", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(CommentNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommentNotFound(CommentNotFoundException ex) {
        return ResponseData.fail("comment  not found", HttpStatus.NOT_FOUND);
    }

    // ================= REFRESH TOKEN =================
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return ResponseData.fail("Invalid refresh token", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredRefreshToken(RefreshTokenExpiredException ex) {
        return ResponseData.fail("Refresh token expired", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ApiResponse<Object>> handleRevokedRefreshToken(RefreshTokenRevokedException ex) {
        return ResponseData.fail("Refresh token revoked", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseData.fail("Invalid username or password", HttpStatus.UNAUTHORIZED);
    }

    // ================= CLOUDINARY =================
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidImage(InvalidImageException ex) {
        return ResponseData.fail("Invalid image", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleUpload(CloudinaryUploadException ex) {

        log.error("CLOUDINARY_UPLOAD_FAILED | userId={} | msg={}",
                getUserId(),
                ex.getMessage()
        );

        return ResponseData.fail("Upload failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<ApiResponse<Object>> handleDelete(CloudinaryDeleteException ex) {

        log.error("CLOUDINARY_DELETE_FAILED | userId={} | msg={}",
                getUserId(),
                ex.getMessage()
        );

        return ResponseData.fail("Delete failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ================= VALIDATION =================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseData.fail("Invalid parameter type", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Validation failed");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {

        Map<String, String> errors = new HashMap<>();
        errors.put("error", ex.getMessage());

        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Invalid argument");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ================= FALLBACK (ONLY IMPORTANT LOG) =================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {

        log.error("INTERNAL_SERVER_ERROR | userId={} | msg={}",
                getUserId(),
                ex.getMessage(),
                ex
        );

        return ResponseData.fail(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}