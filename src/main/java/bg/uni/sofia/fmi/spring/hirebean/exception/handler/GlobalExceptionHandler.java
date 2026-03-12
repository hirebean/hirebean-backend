package bg.uni.sofia.fmi.spring.hirebean.exception.handler;

import bg.uni.sofia.fmi.spring.hirebean.dto.response.ErrorResponse;
import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        ErrorResponse body =
                ErrorResponse.of(ex.getStatus(), ex.getMessage(), request.getRequestURI());

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(err -> err.getField() + ": " + err.getDefaultMessage())
                        .collect(Collectors.joining(", "));

        ErrorResponse body =
                ErrorResponse.of(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        ErrorResponse body =
                ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "An unexpected error occurred",
                        request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
