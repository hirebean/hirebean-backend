package bg.uni.sofia.fmi.spring.hirebean.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Long id) {

        super(resourceName + " with id " + id + " not found", HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String resourceName, String field, String value) {
        super(resourceName + " with " + field + " '" + value + "' not found", HttpStatus.NOT_FOUND);
    }
}
