package web.mvc.exception;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CartException extends RuntimeException implements ErrorCodeProvider{
    private final ErrorCode errorCode;

    @Override
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
