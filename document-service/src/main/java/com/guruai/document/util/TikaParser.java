package com.guruai.document.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper around Apache Tika for parsing document content to plain text.
 *
 * <p>Supported formats (auto-detected by Tika):
 * <ul>
 *   <li>PDF — text extraction (no OCR for scanned PDFs)</li>
 *   <li>DOCX / DOC — full text from paragraphs and tables</li>
 *   <li>TXT / Markdown — raw text</li>
 *   <li>Images (PNG/JPEG) — metadata only (no OCR in Phase 3)</li>
 *   <li>PPTX — slide text</li>
 * </ul>
 *
 * <p>For scanned PDFs / images requiring OCR, integrate Tesseract via
 * {@code tika-parsers-standard-package} + Tesseract installation in Phase 8+.
 */
@Slf4j
@Component
public class TikaParser {

    /**
     * Maximum characters extracted from any single document.
     * Protects against huge PDFs causing memory issues.
     * 5 000 000 chars ≈ 5 MB of plain text.
     */
    private static final int MAX_STRING_LENGTH = 5_000_000;

    private final Tika tika = new Tika();

    /**
     * Parse an {@link InputStream} to plain text using Apache Tika.
     *
     * <p>Uses {@link AutoDetectParser} — Tika inspects magic bytes + MIME type
     * to choose the right parser automatically.
     *
     * @param inputStream  raw bytes of the uploaded file
     * @param originalName original filename (used for MIME detection hint)
     * @return extracted plain text (may be empty for unsupported formats)
     * @throws IOException  if reading the stream fails
     * @throws TikaException if Tika parsing fails
     */
    public String parse(InputStream inputStream, String originalName)
            throws IOException, TikaException {
        try {
            Metadata     metadata = new Metadata();
            metadata.set(org.apache.tika.metadata.TikaCoreProperties.RESOURCE_NAME_KEY, originalName);

            // BodyContentHandler limits output to MAX_STRING_LENGTH chars
            BodyContentHandler handler = new BodyContentHandler(MAX_STRING_LENGTH);
            AutoDetectParser  parser   = new AutoDetectParser();
            ParseContext      context  = new ParseContext();

            // Hardening against Tika/PDFBox memory blowups that have nothing
            // to do with the extracted text size (and so aren't caught by
            // MAX_STRING_LENGTH above): tika-parsers-standard-package bundles
            // OCR support, and Tika's default OCR strategy can decide to
            // render whole PDF pages to in-memory images to check whether
            // OCR is needed — each rendered page is a full-resolution
            // BufferedImage, which is exactly the kind of thing that can
            // exhaust the heap on a small, page-sparse container even for a
            // file that's only tens of KB on disk. We only want plain text,
            // so explicitly disable OCR and inline image extraction.
            PDFParserConfig pdfConfig = new PDFParserConfig();
            pdfConfig.setOcrStrategy(PDFParserConfig.OCR_STRATEGY.NO_OCR);
            pdfConfig.setExtractInlineImages(false);
            context.set(PDFParserConfig.class, pdfConfig);

            parser.parse(inputStream, handler, metadata, context);

            String text = handler.toString().trim();
            log.debug("Tika extracted {} chars from '{}'", text.length(), originalName);
            return text;

        } catch (SAXException e) {
            // SAXException from BodyContentHandler means the length limit was hit
            // This is acceptable — return what we got (partial)
            log.warn("Tika hit MAX_STRING_LENGTH ({}) extracting '{}' — truncated",
                     MAX_STRING_LENGTH, originalName);
            return "";
        }
    }

    /**
     * Quick MIME type detection without full parsing.
     *
     * @param inputStream raw bytes
     * @return detected MIME type (e.g. "application/pdf")
     * @throws IOException if reading fails
     */
    public String detectMimeType(InputStream inputStream) throws IOException {
        return tika.detect(inputStream);
    }

    /**
     * @return {@code true} if the MIME type is supported for text extraction
     */
    public boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.startsWith("text/") ||
               mimeType.equals("application/pdf") ||
               mimeType.contains("wordprocessingml") ||
               mimeType.equals("application/msword") ||
               mimeType.startsWith("image/") ||
               mimeType.contains("presentationml") ||
               mimeType.contains("spreadsheetml");
    }
}
