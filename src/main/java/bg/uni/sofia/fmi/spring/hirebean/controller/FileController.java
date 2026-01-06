package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.service.S3Service;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/files")
@RequiredArgsConstructor
public class FileController {

  private final S3Service s3Service;

  @PostMapping("/upload")
  public ResponseEntity<Map<String, String>> uploadFile(
      @RequestParam("file") MultipartFile file, @RequestParam("folder") String folder) {
    String key = s3Service.uploadFile(file, folder);
    String publicUrl = s3Service.getPublicUrl(key);
    return ResponseEntity.ok(Map.of("key", key, "publicUrl", publicUrl));
  }
}
