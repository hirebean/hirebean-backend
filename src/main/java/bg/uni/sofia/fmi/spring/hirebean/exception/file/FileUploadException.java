package bg.uni.sofia.fmi.spring.hirebean.exception.file;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class FileUploadException extends BusinessException {
    public FileUploadException(String message) {
        super("File upload failed: " + message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
