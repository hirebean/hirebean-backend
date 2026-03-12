package bg.uni.sofia.fmi.spring.hirebean.exception.auth;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }

    public ForbiddenException() {
        super("You don't have permission to access this resource.", HttpStatus.FORBIDDEN);
    }
}
