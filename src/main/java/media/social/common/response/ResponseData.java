package media.social.common.response;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

public class ResponseData {

    public static <T> ResponseEntity<ApiResponse<T>> success(
            T data,
            String message,
            HttpStatus status
    ) {
        ApiResponse<T> res = new ApiResponse<>();
        res.setSuccess(true);
        res.setMessage(message);
        res.setData(data);
        return new ResponseEntity<>(res, status);
    }

    public static <T> ResponseEntity<ApiResponse<List<T>>> successPaginate(
            Page<T> page,
            String message,
            HttpStatus status
    ) {
        ApiResponse<List<T>> res = new ApiResponse<>();
        res.setSuccess(true);
        res.setMessage(message);
        res.setData(page.getContent());
        res.setPagination(PaginationResponse.fromPage(page));

        return new ResponseEntity<>(res, status);
    }

    public static ResponseEntity<ApiResponse<Object>> fail(
            String message,
            HttpStatus status
    ) {
        ApiResponse<Object> res = new ApiResponse<>();
        res.setSuccess(false);
        res.setMessage(message);
        return new ResponseEntity<>(res, status);
    }
}