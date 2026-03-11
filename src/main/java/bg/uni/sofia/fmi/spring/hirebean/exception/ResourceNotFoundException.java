package bg.uni.sofia.fmi.spring.hirebean.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String resourceName, Long id) {

        super(resourceName + " with id " + id + " not found", HttpStatus.NOT_FOUND);
    }
}
