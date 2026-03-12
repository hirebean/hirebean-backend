package bg.uni.sofia.fmi.spring.hirebean.exception.company;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class CompanyNotFoundException extends BusinessException {
    public CompanyNotFoundException(String message) {

        super(message, HttpStatus.NOT_FOUND);
    }
}
