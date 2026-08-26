package bg.uni.sofia.fmi.spring.hirebean.exception.file;

import bg.uni.sofia.fmi.spring.hirebean.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class FileDownloadException extends BusinessException {
    public FileDownloadException(String message) {
        super("File download failed: " + message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
