package bg.uni.sofia.fmi.spring.hirebean.service;

import bg.uni.sofia.fmi.spring.hirebean.dto.request.AiPromptRequest;
import bg.uni.sofia.fmi.spring.hirebean.dto.response.AiPromptResponse;

public interface AiAssistantService {

    AiPromptResponse generate(AiPromptRequest request);
}
