package media.social.common.exception;

import lombok.extern.slf4j.Slf4j;
import media.social.common.response.ApiResponse;
import media.social.common.response.ResponseData;
import media.social.modults.exception.cloudinary.CloudinaryDeleteException;
import media.social.modults.exception.cloudinary.CloudinaryUploadException;
import media.social.modults.exception.cloudinary.InvalidImageException;
import media.social.modults.exception.like.LikeAlreadyExistsException;
import media.social.modults.exception.like.LikeNotFoundException;
import media.social.modults.exception.post.InvalidDateRangeException;
import media.social.modults.exception.post.PostNotFoundException;
import media.social.modults.exception.refreshtoken.InvalidRefreshTokenException;
import media.social.modults.exception.refreshtoken.RefreshTokenExpiredException;
import media.social.modults.exception.refreshtoken.RefreshTokenRevokedException;
import media.social.modults.exception.user.UserAlreadyExistsException;
import media.social.modults.exception.user.UserNotFoundException;
import media.social.modults.security.userdetails.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // ================= GET USER ID SAFE =================
    private Long getUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth == null
                    || !auth.isAuthenticated()
                    || auth.getPrincipal() == null
                    || auth.getPrincipal().equals("anonymousUser")) {
                return null;
            }

            Object principal = auth.getPrincipal();

            if (principal instanceof CustomUserDetails userDetails) {
                return userDetails.getId();
            }

            return null;

        } catch (Exception e) {
            log.warn("Failed to extract userId from SecurityContext", e);
            return null;
        }
    }

    // ================= COMMON LOG =================
    private void logError(String message, Exception ex) {
        log.error(
                "{} | userId={}",
                message,
                getUserId(),
                ex
        );
    }

    // ================= USER =================
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UserNotFoundException ex) {
        logError("USER_NOT_FOUND", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        logError("USER_ALREADY_EXISTS", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    // ================= POST =================
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostNotFound(PostNotFoundException ex) {
        logError("POST_NOT_FOUND", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidDateRangeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidDateRange(InvalidDateRangeException ex) {
        logError("INVALID_DATE_RANGE", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    // ================= LIKE =================
    @ExceptionHandler(LikeAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleLikeAlreadyExists(LikeAlreadyExistsException ex) {
        logError("LIKE_ALREADY_EXISTS", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(LikeNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleLikeNotFound(LikeNotFoundException ex) {
        logError("LIKE_NOT_FOUND", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // ================= REFRESH TOKEN =================
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        logError("INVALID_REFRESH_TOKEN", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleExpiredRefreshToken(RefreshTokenExpiredException ex) {
        logError("REFRESH_TOKEN_EXPIRED", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ApiResponse<Object>> handleRevokedRefreshToken(RefreshTokenRevokedException ex) {
        logError("REFRESH_TOKEN_REVOKED", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    // ================= CLOUDINARY =================
    @ExceptionHandler(InvalidImageException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidImage(InvalidImageException ex) {
        logError("INVALID_IMAGE", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(CloudinaryUploadException.class)
    public ResponseEntity<ApiResponse<Object>> handleUpload(CloudinaryUploadException ex) {
        logError("CLOUDINARY_UPLOAD_FAILED", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CloudinaryDeleteException.class)
    public ResponseEntity<ApiResponse<Object>> handleDelete(CloudinaryDeleteException ex) {
        logError("CLOUDINARY_DELETE_FAILED", ex);
        return ResponseData.fail(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ================= VALIDATION =================
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

        logError("TYPE_MISMATCH", ex);

        String message;

        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            message = String.format(
                    "Invalid value '%s'. Allowed: %s",
                    ex.getValue(),
                    Arrays.toString(ex.getRequiredType().getEnumConstants())
            );
        } else {
            message = String.format(
                    "Invalid value '%s' for parameter '%s'",
                    ex.getValue(),
                    ex.getName()
            );
        }

        return ResponseData.fail(message, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {

        logError("VALIDATION_ERROR", ex);

        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ApiResponse<Object> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage("Validation failed");
        response.setData(errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // ================= FALLBACK =================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {

        log.error(
                "INTERNAL_SERVER_ERROR | userId={}",
                getUserId(),
                ex
        );

        return ResponseData.fail(
                "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}