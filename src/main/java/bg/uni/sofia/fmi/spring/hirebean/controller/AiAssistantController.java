package bg.uni.sofia.fmi.spring.hirebean.controller;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.AiPromptRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.AiPromptResponse;
import bg.uni.sofia.fmi.spring.hirebean.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/prompt")
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER','ADMIN')")
    public ResponseEntity<AiPromptResponse> generate(@Valid @RequestBody AiPromptRequest request) {
        return ResponseEntity.ok(aiAssistantService.generate(request));
    }
}
