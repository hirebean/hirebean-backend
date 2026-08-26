package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.service.StorageService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file, @RequestParam("folder") String folder) {
        Map<String, String> responseBody = new HashMap<>();
        String key = storageService.uploadFile(file, folder);
        responseBody.put("key", key);
        String publicUrl = storageService.getPublicUrl(key);
        if (publicUrl != null) {
            responseBody.put("publicUrl", publicUrl);
        }
        return ResponseEntity.ok(responseBody);
    }
}
