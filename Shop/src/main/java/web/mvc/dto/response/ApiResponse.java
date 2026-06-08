package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "요청이 성공했습니다.",
                data
        );
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(
                HttpStatus.CREATED.value(),
                "생성되었습니다.",
                data
        );
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "요청이 성공했습니다.",
                null
        );
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                message,
                null
        );
    }
}