package media.social.common.response;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "success",
        "message",
        "data",
        "pagination"
})
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private PaginationResponse pagination;
}