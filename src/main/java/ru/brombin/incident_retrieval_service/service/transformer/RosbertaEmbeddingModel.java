package ru.brombin.incident_retrieval_service.service.transformer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RosbertaEmbeddingModel implements EmbeddingModel {
    RestClient restClient;

    @Override
    public @NotNull EmbeddingResponse call(@NotNull EmbeddingRequest request) {
        try {
            var payload = Map.of(
                    "inputs", "search_query: " + request.getInstructions().get(0),
                    "parameters", Map.of("pooling_method", "cls", "normalize_embeddings", true)
            );

            List<Double> responseList = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (responseList == null || responseList.isEmpty()) {
                throw new IllegalStateException("Empty embedding response");
            }

            float[] floats = responseList.stream()
                    .mapToDouble(Double::doubleValue)
                    .collect(() -> new FloatArrayBuilder(responseList.size()),
                            FloatArrayBuilder::add,
                            FloatArrayBuilder::addAll)
                    .toArray();

            return new EmbeddingResponse(List.of(new Embedding(floats, 0)));
        } catch (RestClientException ex) {
            throw new RuntimeException("Hugging Face API call failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public @NotNull float[] embed(@NotNull Document document) {
        log.info("Embedding document: {}", document.getFormattedContent());
        EmbeddingResponse response = call(
                new EmbeddingRequest(List.of(document.getFormattedContent()), null)
        );
        float[] vector = response.getResults().get(0).getOutput();
        log.info("Document embedding length: {}, first 5 elements: {}", vector.length,
                Arrays.toString(Arrays.copyOf(vector, Math.min(vector.length, 5))));
        return vector;
    }

    static class FloatArrayBuilder {
        final float[] data;
        int pos = 0;
        FloatArrayBuilder(int size) { data = new float[size]; }
        void add(double v) { data[pos++] = (float)v; }
        void addAll(FloatArrayBuilder o) {
            System.arraycopy(o.data, 0, data, pos, o.pos);
            pos += o.pos;
        }
        float[] toArray() { return data; }
    }
}
