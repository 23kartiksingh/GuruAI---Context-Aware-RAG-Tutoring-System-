package com.guruai.document.mapper;

import com.guruai.document.dto.response.DocumentResponse;
import com.guruai.document.entity.UserDocument;
import org.springframework.stereotype.Component;

/**
 * Converts {@link UserDocument} entities to response DTOs.
 */
@Component
public class DocumentMapper {

    public DocumentResponse toResponse(UserDocument doc) {
        return new DocumentResponse(
                doc.getId().toString(),
                doc.getSessionId().toString(),
                doc.getFilename(),
                doc.getFileType(),
                Math.round(doc.getFileSizeBytes() / 1024.0 / 1024.0 * 100.0) / 100.0,
                doc.getChunkCount(),
                doc.getStatus(),
                doc.getTopics(),
                doc.getSubject(),
                doc.getCreatedAt()
        );
    }
}
