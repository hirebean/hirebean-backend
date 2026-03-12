package bg.uni.sofia.fmi.spring.hirebean.exception.auth;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(String message) {

        super(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException() {
        super("You are Unauthorized. Please log in.", HttpStatus.UNAUTHORIZED);
    }
}
