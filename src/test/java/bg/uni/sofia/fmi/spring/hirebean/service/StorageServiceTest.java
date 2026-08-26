package bg.uni.sofia.fmi.spring.hirebean.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StorageServiceTest {

    private HttpServer server;
    private StorageService storageService;
    private String baseUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        storageService = new StorageService(new ObjectMapper());
        ReflectionTestUtils.setField(storageService, "supabaseUrl", baseUrl);
        ReflectionTestUtils.setField(storageService, "serviceKey", "test-service-key");
        ReflectionTestUtils.setField(storageService, "publicBucket", "hirebean-public");
        ReflectionTestUtils.setField(storageService, "privateBucket", "hirebean-private");
        ReflectionTestUtils.setField(storageService, "signedUrlSeconds", 600);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void getPresignedUrl_resolvesSupabaseRelativeSignedUrlUnderStorageV1() {
        respondWith(
                "/storage/v1/object/sign/hirebean-private/cvs/candidate.pdf",
                "{\"signedURL\":\"/object/sign/hirebean-private/cvs/candidate.pdf?token=abc123\"}");

        String result = storageService.getPresignedUrl("cvs/candidate.pdf");

        assertThat(result)
                .isEqualTo(baseUrl + "/storage/v1/object/sign/hirebean-private/cvs/candidate.pdf?token=abc123");
    }

    @Test
    void getPresignedUrl_keepsAlreadyStorageV1PrefixedSignedUrlAsIs() {
        respondWith(
                "/storage/v1/object/sign/hirebean-private/cvs/candidate.pdf",
                "{\"signedURL\":\"/storage/v1/object/sign/hirebean-private/cvs/candidate.pdf?token=abc123\"}");

        String result = storageService.getPresignedUrl("cvs/candidate.pdf");

        assertThat(result)
                .isEqualTo(baseUrl + "/storage/v1/object/sign/hirebean-private/cvs/candidate.pdf?token=abc123");
    }

    @Test
    void getPresignedUrl_passesThroughAbsoluteSignedUrl() {
        String absoluteUrl = "https://cdn.example.com/object/sign/hirebean-private/cvs/candidate.pdf?token=abc123";
        respondWith(
                "/storage/v1/object/sign/hirebean-private/cvs/candidate.pdf",
                "{\"signedURL\":\"" + absoluteUrl + "\"}");

        String result = storageService.getPresignedUrl("cvs/candidate.pdf");

        assertThat(result).isEqualTo(absoluteUrl);
    }

    private void respondWith(String path, String jsonBody) {
        server.createContext(path, exchange -> {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }
}
