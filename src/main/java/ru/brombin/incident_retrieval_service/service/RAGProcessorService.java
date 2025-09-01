package ru.brombin.incident_retrieval_service.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import ru.brombin.incident_retrieval_service.entity.PreprocessedQuestion;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RAGProcessorService {

    OpenAiLLMService llmService;
    IncidentEmbeddingService incidentEmbeddingService;

    String retrievalUrl = "http://localhost:9090/api/v1/retrieval/incident/similar";

    public List<Document> processUserQuery(String userQuery) {
        log.info("Начало обработки запроса: {}", userQuery);

        // 1. Предобрабатываем вопрос через LLM
        PreprocessedQuestion preprocessed = llmService.getPreprocessedRequest(userQuery);
        log.info("Предобработанные варианты вопроса: {}", preprocessed.normalized(), preprocessed.variants());

        // 2. Собираем документы по каждому варианту
        List<Document> allDocuments = new ArrayList<>();
        for (String variant : preprocessed.variants()) {
            log.info("Запрос к retrieval API для варианта: {}", variant);
            List<Document> docs = fetchSimilarDocuments(variant);
            log.info("Документы, полученные для варианта: {}", docs.size());
            allDocuments.addAll(docs);
        }

        // 3. Убираем дубликаты и ранжируем
        List<Document> rankedDocuments = rankDocuments(allDocuments);
        log.info("Общее количество документов после ранжирования: {}", rankedDocuments.size());

        return rankedDocuments;
    }

    private List<Document> fetchSimilarDocuments(String queryVariant) {
        int topK = 3;

        // Выполняем поиск в QdrantVectorStore
        List<Document> results = incidentEmbeddingService.searchSimilarDocuments(queryVariant, topK);

        log.info("Найдено {} похожих документов", results.size());

        return results;
    }

    private List<Document> rankDocuments(List<Document> documents) {
        // Убираем дубликаты по уникальному идентификатору документа
        Map<String, Document> uniqueDocs = documents.stream()
                .collect(Collectors.toMap(
                        Document::getId,
                        d -> d,
                        (d1, d2) -> d1.getScore() >= d2.getScore() ? d1 : d2
                ));

        List<Document> ranked = uniqueDocs.values().stream()
                .sorted(Comparator.comparingDouble(Document::getScore).reversed())
                .toList();

        log.info("Документы после удаления дубликатов и сортировки: {}", ranked.size());
        return ranked;
    }

    public String generateAugmentedAnswer(String userQuery) {
        log.info("Генерация ответа для запроса: {}", userQuery);

        List<Document> topDocuments = processUserQuery(userQuery).stream()
                .limit(3)
                .toList();

        String context = topDocuments.stream()
                .limit(3)
                .map(Document::getFormattedContent)
                .collect(Collectors.joining("\n\n"));

        log.info("Контекст для LLM:\n{}", context);

        String answer = llmService.generateAnswerFromContext(userQuery, context);
        log.info("Сформированный ответ: {}", answer);

        return answer;
    }

    public String generateAnswer(String userQuery) {
        log.info("Генерация ответа для запроса: {}", userQuery);

        return llmService.generateAnswerFromContext(userQuery, "");
    }
}

