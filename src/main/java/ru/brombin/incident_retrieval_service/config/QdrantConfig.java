package ru.brombin.incident_retrieval_service.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.brombin.incident_retrieval_service.service.transformer.RosbertaEmbeddingModel;

@Slf4j
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QdrantConfig {

    @Value("${qdrant.host:localhost}")
    String qdrantHost;

    @Value("${qdrant.port:6334}")
    int qdrantPort;

    @Value("${qdrant.collection-name:incidents}")
    String collectionName;

    @Value("${qdrant.api-key:}")
    String apiKey;

    @Bean
    @Primary
    public QdrantClient qdrantClient() {
        log.info("Initializing Qdrant client: {}:{} collection:{}", qdrantHost, qdrantPort, collectionName);
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false);
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            builder.withApiKey(apiKey);
            log.info("Using API key for Qdrant authentication");
        } else {
            log.info("No API key provided, using unauthenticated connection");
        }
        return new QdrantClient(builder.build());
    }

    @Bean
    @Primary
    public QdrantVectorStore qdrantVectorStore(QdrantClient qdrantClient,
                                               RosbertaEmbeddingModel rosbertaEmbeddingModel) {
        log.info("Creating QdrantVectorStore with collection: {}", collectionName);

        QdrantVectorStore voStore = QdrantVectorStore.builder(qdrantClient, rosbertaEmbeddingModel)
                .collectionName(collectionName)
                .initializeSchema(true)
                .build();

        log.info("QdrantVectorStore created successfully");
        return voStore;
    }
}
