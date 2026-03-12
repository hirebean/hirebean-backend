package bg.uni.sofia.fmi.spring.hirebean.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        int status,
        String error,
        String message,
        String path,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                LocalDateTime timestamp) {
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(
                status.value(), status.getReasonPhrase(), message, path, LocalDateTime.now());
    }
}
