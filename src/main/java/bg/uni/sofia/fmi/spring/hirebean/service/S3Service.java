package bg.uni.sofia.fmi.spring.hirebean.service;

import io.awspring.cloud.s3.S3Template;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3Service {

  private final S3Template s3Template;
  private final S3Presigner s3Presigner;

  @Value("${spring.cloud.aws.s3.bucket}")
  private String bucketName;

  @Value("${cdn.url}")
  private String cdnUrl;

  //  Upload files and returns its key:
  //  Example: logos/uuid-filename.png
  public String uploadFile(MultipartFile file, String folder) {
    String originalFilename = file.getOriginalFilename();
    String extension =
        originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf('.'))
            : "";

    // preventing collision genering unique name!!!
    String key = folder + "/" + UUID.randomUUID() + extension;
    try {
      s3Template.upload(bucketName, key, file.getInputStream());
      return key;

    } catch (IOException e) {
      throw new RuntimeException("Failed to upload file to S3", e);
    }
  }

  // For public URLS we use CDN!!!
  public String getPublicUrl(String key) {
    if (key == null || key.isBlank()) return null;

    String baseUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
    return baseUrl + "/" + key;
  }

  // For private files such as CV and personal INFO
  public String getPresignedUrl(String key) {
    if (key == null || key.isBlank()) return null;

    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(key).build();

    GetObjectPresignRequest getObjectPresignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(10))
            .getObjectRequest(getObjectRequest)
            .build();

    return s3Presigner.presignGetObject(getObjectPresignRequest).url().toString();
  }
}
