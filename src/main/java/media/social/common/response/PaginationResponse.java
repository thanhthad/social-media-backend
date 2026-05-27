package media.social.common.response;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
@AllArgsConstructor
public class PaginationResponse {
    private int current_page;
    private int per_page;
    private long total;
    private int last_page;
    private int from;
    private int to;

    public static PaginationResponse fromPage(Page<?> page) {
        return new PaginationResponse(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber() * page.getSize() + 1,
                page.getNumber() * page.getSize() + page.getNumberOfElements()
        );
    }
}
