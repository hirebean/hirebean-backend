package bg.uni.sofia.fmi.spring.hirebean.exception.company;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanyAlreadyExistsException extends BusinessException {
    public CompanyAlreadyExistsException(String message) {

        super(message, HttpStatus.CONFLICT);
    }
}
