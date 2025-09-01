package ru.brombin.incident_retrieval_service.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IncidentEmbeddingService {

    QdrantVectorStore qdrantVectorStore;

    public void storeIncident(String text, List<String> tags) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("tags", tags);
        metadata.put("timestamp", System.currentTimeMillis());

        Document document = new Document(text, metadata);
        qdrantVectorStore.doAdd(List.of(document));
        log.info("Новый документ добавлен в векторное хранилище");
    }

    public List<Document> searchSimilarDocuments(String query, Integer limit) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(limit)
                //.filterExtension("key == 'value'")
                //.similarityThreshold(0.6)
                .build();

        log.info("Полученный результат от QdrantVectorStore: {}", searchRequest);
        return qdrantVectorStore.similaritySearch(searchRequest);
    }
}
