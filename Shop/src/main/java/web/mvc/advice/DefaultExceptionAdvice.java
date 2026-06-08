package web.mvc.advice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import web.mvc.dto.response.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import web.mvc.exception.ErrorCode;
import web.mvc.exception.ErrorCodeProvider;

import java.time.LocalDateTime;

@RestControllerAdvice
public class DefaultExceptionAdvice {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {

        if (e instanceof ErrorCodeProvider provider) {
            ErrorCode errorCode = provider.getErrorCode();
            return ResponseEntity.status(errorCode.getHttpStatus()).body(ErrorResponse.from(errorCode));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.from(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}