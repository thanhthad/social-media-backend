package media.social.common.exception;

import lombok.extern.slf4j.Slf4j;
import media.social.common.response.ApiResponse;
import media.social.common.response.ResponseData;
import media.social.modults.file.image.exception.CloudinaryDeleteException;
import media.social.modults.file.image.exception.CloudinaryUploadException;
import media.social.modults.file.image.exception.InvalidImageException;
import media.social.modults.post.exception.like.LikeAlreadyExistsException;
import media.social.modults.post.exception.like.LikeNotFoundException;
import media.social.modults.post.exception.post.InvalidDateRangeException;
import media.social.modults.post.exception.post.PostNotFoundException;
import media.social.modults.user.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modults.user.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modults.user.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modults.user.exception.user.UnauthorizedException;
import media.social.modults.user.exception.user.UserAlreadyExistsException;
import media.social.modults.user.exception.user.UserNotFoundException;
import media.social.modults.user.security.userdetails.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ================= SAFE USER ID =================
    private Long getUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null || !auth.isAuthenticated()
                    || auth.getPrincipal() == null
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }

            return null;

        } catch (Exception e) {
            log.warn("SECURITY_CONTEXT_READ_FAILED | {}", shortMsg(e));
            return null;
        }
    }

    // ================= LOG SHORTENER =================
    private String shortMsg(Exception ex) {
        if (ex == null) return "null-error";

        String msg = ex.getMessage();
        if (msg == null) return ex.getClass().getSimpleName();

        return msg.length() > 120 ? msg.substring(0, 120) + "..." : msg;
    }

    private void logError(String code, Exception ex) {
        log.error(
                "{} | userId={} | msg={}",
                code,
                getUserId(),
                shortMsg(ex),
                ex // vẫn giữ stacktrace, nhưng message đã gọn
        );
    }

    // ================= USER =================
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UserNotFoundException ex) {
        logError("USER_NOT_FOUND", ex);
        return ResponseData.fail("User not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        logError("USER_ALREADY_EXISTS", ex);
        return ResponseData.fail("User already exists", HttpStatus.CONFLICT);
    }

    // ================= POST =================
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostNotFound(PostNotFoundException ex) {
        logError("POST_NOT_FOUND", ex);
        return ResponseData.fail("Post not found", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        logError("INVALID_DATE_RANGE", ex);
        return ResponseData.fail("Invalid date range", HttpStatus.BAD_REQUEST);
    }

    // ================= LIKE =================
    @ExceptionHandler(LikeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleLikeAlreadyExists(LikeAlreadyExistsException ex) {
        logError("LIKE_ALREADY_EXISTS", ex);
        return ResponseData.fail("Already liked", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LikeNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleLikeNotFound(LikeNotFoundException ex) {
        logError("LIKE_NOT_FOUND", ex);
        return ResponseData.fail("Like not found", HttpStatus.NOT_FOUND);
    }

    // ================= REFRESH TOKEN =================
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        logError("INVALID_REFRESH_TOKEN", ex);
        return ResponseData.fail("Invalid refresh token", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredRefreshToken(RefreshTokenExpiredException ex) {
        logError("REFRESH_TOKEN_EXPIRED", ex);
        return ResponseData.fail("Refresh token expired", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ApiResponse<Object>> handleRevokedRefreshToken(RefreshTokenRevokedException ex) {
        logError("REFRESH_TOKEN_REVOKED", ex);
        return ResponseData.fail("Refresh token revoked", HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {

        log.warn("BAD_CREDENTIALS | userId={} | msg={}", getUserId(), shortMsg(ex));

        return ResponseData.fail(
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED
        );
    }

    // ================= CLOUDINARY =================
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidImage(InvalidImageException ex) {
        logError("INVALID_IMAGE", ex);
        return ResponseData.fail("Invalid image", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleUpload(CloudinaryUploadException ex) {
        logError("CLOUDINARY_UPLOAD_FAILED", ex);
        return ResponseData.fail("Upload failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<ApiResponse<Object>> handleDelete(CloudinaryDeleteException ex) {
        logError("CLOUDINARY_DELETE_FAILED", ex);
        return ResponseData.fail("Delete failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(UnauthorizedException ex) {
        logError("UNAUTHORIZED", ex);
        return ResponseData.fail("Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    // ================= VALIDATION =================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        logError("TYPE_MISMATCH", ex);

        String message;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = "Invalid enum value. Allowed: "
                    + Arrays.toString(ex.getRequiredType().getEnumConstants());
        } else {
            message = "Invalid parameter type: " + ex.getName();
        }

        return ResponseData.fail(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

        log.warn("VALIDATION_ERROR | userId={}", getUserId());

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors()
                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Validation failed");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ================= FALLBACK =================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {

        log.error("INTERNAL_SERVER_ERROR | userId={} | msg={}",
                getUserId(),
                shortMsg(ex),
                ex
        );

        return ResponseData.fail(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}