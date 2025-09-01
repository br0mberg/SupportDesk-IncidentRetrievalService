package ru.brombin.incident_retrieval_service.controller;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.brombin.incident_retrieval_service.service.IncidentEmbeddingService;
import ru.brombin.incident_retrieval_service.service.transformer.RosbertaEmbeddingModel;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/retrieval")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RetrievalIncidentController {

    RosbertaEmbeddingModel rosbertaEmbeddingModel;
    IncidentEmbeddingService incidentEmbeddingService;

    @PostMapping(path = "/transformer", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<float[]> getEmbeddingTransformation(@RequestBody String text) {
        try {
            Document document = new Document(text);
            float[] embedding = rosbertaEmbeddingModel.embed(document);
            return ResponseEntity.ok(embedding);
        } catch (Exception e) {
            log.error("Ошибка при генерации эмбеддинга для текста: {}", text, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(path = "/incidents", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> add(@RequestBody String text, @RequestParam(required = false) List<String> tags) {
        try {
            incidentEmbeddingService.storeIncident(text, tags);
            return ResponseEntity.ok("Incident успешно добавлен в векторное хранилище");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(path = "/incidents/similar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Document>> getSimilarIncidents(
            @RequestBody String query,
            @RequestParam(defaultValue = "3") int limit) {
        try {
            List<Document> responseDocuments = incidentEmbeddingService.searchSimilarDocuments(query, limit);
            log.info("Поиск похожих инцидентов по запросу '{}': найдено {} результатов", query, responseDocuments.size());
            return ResponseEntity.ok(responseDocuments);
        } catch (Exception e) {
            log.error("Ошибка при поиске похожих инцидентов по запросу '{}'", query, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
