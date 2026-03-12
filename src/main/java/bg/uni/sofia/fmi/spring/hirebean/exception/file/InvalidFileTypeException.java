package bg.uni.sofia.fmi.spring.hirebean.exception.file;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidFileTypeException extends BusinessException {
    public InvalidFileTypeException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
