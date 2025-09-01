package ru.brombin.incident_retrieval_service.controller;


import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.brombin.incident_retrieval_service.service.OpenAiLLMService;
import ru.brombin.incident_retrieval_service.service.RAGProcessorService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AugmentedGenerationController {

    OpenAiLLMService openAiLLMService;
    RAGProcessorService ragProcessorService;

    @PostMapping("/augmented-generation/question")
    public String getAugmentedAnswer(@RequestBody String request) {
        return ragProcessorService.generateAugmentedAnswer(request);
    }

    @PostMapping("/question")
    public String getAnswer(@RequestBody String request) {
        return ragProcessorService.generateAnswer(request);
    }
}
