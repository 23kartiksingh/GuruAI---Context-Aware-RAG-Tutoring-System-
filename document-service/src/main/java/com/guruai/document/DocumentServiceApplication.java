package com.guruai.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * GuruAI Document Service — Port 8082  |  DB: document_db (PostgreSQL + pgvector)
 *
 * <p>Handles all document ingestion and vector search operations.
 *
 * <p>AI provider: Google Gemini ONLY (single provider per service — see root
 * README's provider-split section). VectorStore + EmbeddingModel + ChatModel
 * all come from Spring AI auto-configuration; there are deliberately no
 * manual AI config classes here, which only works because exactly one
 * provider starter is on the classpath.
 *
 * <p>Package structure:
 * <pre>
 * config/
 *   ├── DocumentProperties.java    — chunking / upload limits binding
 *   ├── InternalAccessConfig.java  — gateway-only access filter registration
 *   ├── KafkaProducerConfig.java
 *   └── RedisConfig.java           — LRU retriever cache (32-slot, ported from Python)
 *
 * controller/
 *   └── DocumentController.java    — /documents/{sessionId}/upload, /search, /status, DELETE
 *
 * service/
 *   ├── DocumentService.java       — interface
 *   ├── ChunkingService.java       — interface
 *   ├── HybridSearchService.java   — interface (Dense + BM25 + RRF)
 *   └── impl/
 *       ├── DocumentServiceImpl.java
 *       ├── ChunkingServiceImpl.java   — 1000-char / 150-overlap chunking
 *       └── HybridSearchServiceImpl.java — pgvector + BM25 + Reciprocal Rank Fusion
 *
 * repository/
 *   ├── DocumentRepository.java
 *   └── ChunkRepository.java
 *
 * entity/
 *   ├── Document.java              — documents table (metadata)
 *   └── Chunk.java                 — chunks table (content + embedding ref)
 *
 * dto/
 *   ├── request/
 *   │   └── SearchRequest.java
 *   └── response/
 *       ├── DocumentResponse.java
 *       ├── ChunkResponse.java     — content + page_number + similarity_score
 *       └── ConceptMapResponse.java — AI-extracted topics from document
 *
 * event/
 *   └── producer/
 *       └── DocumentEventProducer.java — publishes DocumentIndexedEvent
 *
 * exception/
 *   └── GlobalExceptionHandler.java
 *
 * mapper/
 *   └── DocumentMapper.java
 *
 * util/
 *   ├── TikaParser.java            — Apache Tika PDF/DOCX/TXT/image parsing
 *   └── RrfMerger.java             — Reciprocal Rank Fusion algorithm (from Python retriever.py)
 * </pre>
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
