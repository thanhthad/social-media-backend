package media.social.common.exception;


import lombok.extern.slf4j.Slf4j;
import media.social.common.response.ApiResponse;
import media.social.common.response.ResponseData;
import media.social.modults.exception.post.PostNotFoundException;
import media.social.modults.exception.user.UserAlreadyExistsException;
import media.social.modults.exception.user.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHanler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(
            UserNotFoundException ex
    ){
        return ResponseData.fail(ex.getMessage(),HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex
    ){
        return ResponseData.fail(ex.getMessage(),HttpStatus.CONFLICT);
    }
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePostNotFound(
            PostNotFoundException ex
    ){
        return ResponseData.fail(ex.getMessage(),HttpStatus.NOT_FOUND);
    }
}
