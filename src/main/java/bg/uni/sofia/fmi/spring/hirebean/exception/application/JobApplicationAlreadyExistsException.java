package bg.uni.sofia.fmi.spring.hirebean.exception.application;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class JobApplicationAlreadyExistsException extends BusinessException {

    private static final String MESSAGE = "You have already applied for this job.";

    public JobApplicationAlreadyExistsException() {
        super(MESSAGE, HttpStatus.CONFLICT);
    }
}
