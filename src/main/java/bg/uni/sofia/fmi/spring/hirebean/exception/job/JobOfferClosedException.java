package bg.uni.sofia.fmi.spring.hirebean.exception.job;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JobOfferClosedException extends BusinessException {
    public JobOfferClosedException(String message) {

        super(message, HttpStatus.BAD_REQUEST);
    }
}
