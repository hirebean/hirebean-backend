package bg.uni.sofia.fmi.spring.hirebean.exception.job;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JobOfferNotFoundException extends BusinessException {
    public JobOfferNotFoundException(String message) {

        super(message, HttpStatus.NOT_FOUND);
    }
}
