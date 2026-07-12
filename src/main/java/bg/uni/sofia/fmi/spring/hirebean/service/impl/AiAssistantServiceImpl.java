package bg.uni.sofia.fmi.spring.hirebean.service.impl;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.AiPromptRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.AiPromptResponse;
import bg.uni.sofia.fmi.spring.hirebean.service.AiAssistantService;
import bg.uni.sofia.fmi.spring.hirebean.service.AuditLogService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AiAssistantServiceImpl implements AiAssistantService {

    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public AiPromptResponse generate(AiPromptRequest request) {
        String prompt = request.getPrompt().trim();
        String purpose =
                StringUtils.hasText(request.getPurpose()) ? request.getPurpose().trim() : "general";
        String response = buildLocalResponse(prompt, purpose);

        auditLogService.record("PROMPT", "AiAssistant", null, "Generated local AI assistant draft", "INFO");

        return AiPromptResponse.builder()
                .prompt(prompt)
                .response(response)
                .provider("LOCAL_TEMPLATE")
                .createdAt(LocalDateTime.now())
                .build();
    }

    private String buildLocalResponse(String prompt, String purpose) {
        String normalizedPurpose = purpose.toLowerCase();
        String normalizedPrompt = prompt.toLowerCase();

        if (normalizedPurpose.contains("job") || normalizedPrompt.contains("job")) {
            return "Draft job description:\n\n"
                    + "Role summary: "
                    + prompt
                    + "\n\nResponsibilities:\n"
                    + "- Own the core delivery for the role.\n"
                    + "- Collaborate with product, engineering, and business stakeholders.\n"
                    + "- Communicate progress clearly and keep quality high.\n\nRequirements:\n"
                    + "- Strong practical experience with the relevant technologies.\n"
                    + "- Good problem-solving, ownership, and teamwork.\n"
                    + "- Ability to work in a fast-moving environment.";
        }

        if (normalizedPurpose.contains("cover") || normalizedPrompt.contains("cover letter")) {
            return "Cover letter draft:\n\n"
                    + "Hello,\n\n"
                    + "I am excited to apply for this opportunity. My background matches the role, and I can bring "
                    + "clear communication, practical problem-solving, and ownership from day one.\n\n"
                    + "The part of the role that stands out to me is: "
                    + prompt
                    + "\n\n"
                    + "Best regards";
        }

        return "Career assistant draft:\n\n"
                + "Based on your prompt, focus on one clear goal, show concrete examples, and keep the message specific. "
                + "Prompt summary: "
                + prompt;
    }
}
