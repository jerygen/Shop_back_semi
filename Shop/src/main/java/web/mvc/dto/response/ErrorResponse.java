package web.mvc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import web.mvc.exception.ErrorCode;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private String code;

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getHttpStatus().value(),
                errorCode.getTitle(),
                errorCode.getMessage()
        );
    }
}
