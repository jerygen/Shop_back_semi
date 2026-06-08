package web.mvc.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserException extends RuntimeException implements ErrorCodeProvider{
    private final ErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
