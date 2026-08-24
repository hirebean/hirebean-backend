package bg.uni.sofia.fmi.spring.hirebean.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class StorageService {

    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final int DEFAULT_SIGNED_URL_SECONDS = 600;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Value("${storage.supabase-url}")
    private String supabaseUrl;

    @Value("${storage.service-key}")
    private String serviceKey;

    @Value("${storage.public-bucket:hirebean-public}")
    private String publicBucket;

    @Value("${storage.private-bucket:hirebean-private}")
    private String privateBucket;

    @Value("${storage.signed-url-seconds:600}")
    private int signedUrlSeconds;

    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file, folder);

        String extension = extensionOf(file.getOriginalFilename());
        String key = folder.trim() + "/" + UUID.randomUUID() + extension;
        String bucket = bucketForFolder(folder);
        String endpoint = objectEndpoint(bucket, key);

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("Content-Type", contentTypeOf(file))
                    .header("x-upsert", "false")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "upload", bucket, key);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload file to Supabase Storage", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("File upload was interrupted", e);
        }
    }

    public String getPublicUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.startsWith("https://") || key.startsWith("http://")) {
            return key;
        }
        String bucket = bucketForKey(key);
        if (!publicBucket.equals(bucket)) {
            return null;
        }
        return trimTrailingSlash(supabaseUrl) + "/storage/v1/object/public/" + encodePath(bucket) + "/"
                + encodePath(key);
    }

    public String getPresignedUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String bucket = bucketForKey(key);
        int expiresIn = signedUrlSeconds > 0 ? signedUrlSeconds : DEFAULT_SIGNED_URL_SECONDS;
        String endpoint = storageBaseUrl() + "/object/sign/" + encodePath(bucket) + "/" + encodePath(key);
        String body = "{\"expiresIn\":" + expiresIn + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + serviceKey)
                    .header("apikey", serviceKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ensureSuccess(response, "sign URL", bucket, key);
            JsonNode payload = objectMapper.readTree(response.body());
            String signedUrl = textValue(payload, "signedURL", "signedUrl");
            if (signedUrl == null) {
                throw new IllegalStateException("Supabase Storage returned no signed URL");
            }
            return signedUrl.startsWith("http") ? signedUrl : trimTrailingSlash(supabaseUrl) + signedUrl;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid signed URL response from Supabase Storage", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create signed URL from Supabase Storage", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Signed URL request was interrupted", e);
        }
    }

    private void validateFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be null or empty.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("File exceeds the maximum allowed size of 10 MB.");
        }
        if (folder == null || folder.isBlank() || folder.contains("..") || folder.contains("/")) {
            throw new IllegalArgumentException("Invalid storage folder.");
        }
    }

    private String bucketForFolder(String folder) {
        return isPublicFolder(folder) ? publicBucket : privateBucket;
    }

    private String bucketForKey(String key) {
        int slashIndex = key.indexOf('/');
        String folder = slashIndex > 0 ? key.substring(0, slashIndex) : key;
        return isPublicFolder(folder) ? publicBucket : privateBucket;
    }

    private boolean isPublicFolder(String folder) {
        return switch (folder) {
            case "profile-pictures", "company-logos", "post-images" -> true;
            default -> false;
        };
    }

    private String storageBaseUrl() {
        return trimTrailingSlash(supabaseUrl) + "/storage/v1";
    }

    private String objectEndpoint(String bucket, String key) {
        return storageBaseUrl() + "/object/" + encodePath(bucket) + "/" + encodePath(key);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).replaceAll("[^a-zA-Z0-9.]", "");
    }

    private String contentTypeOf(MultipartFile file) {
        return file.getContentType() == null || file.getContentType().isBlank()
                ? "application/octet-stream"
                : file.getContentType();
    }

    private String textValue(JsonNode payload, String... names) {
        for (String name : names) {
            JsonNode value = payload.get(name);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return null;
    }

    private void ensureSuccess(HttpResponse<String> response, String operation, String bucket, String key) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Supabase Storage " + operation + " failed with HTTP status "
                    + response.statusCode() + " for bucket " + bucket + " and key " + key);
        }
    }
}
