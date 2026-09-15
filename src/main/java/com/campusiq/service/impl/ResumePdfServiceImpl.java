package com.campusiq.service.impl;

import java.io.IOException;
import java.util.Locale;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.campusiq.service.ResumePdfService;

@Service
public class ResumePdfServiceImpl
        implements ResumePdfService {

    private static final long MAX_FILE_SIZE =
            5 * 1024 * 1024;

    private static final int MAX_TEXT_LENGTH =
            50000;

    @Override
    public String extractText(
            MultipartFile file) {

        validateFile(file);

        try (
                PDDocument document =
                        Loader.loadPDF(
                                file.getBytes()
                        )
        ) {

            PDFTextStripper stripper =
                    new PDFTextStripper();

            String text =
                    stripper.getText(document);

            if (text == null ||
                    text.isBlank()) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Resume PDF does not contain readable text"
                );
            }

            text = text.trim();

            if (text.length() >
                    MAX_TEXT_LENGTH) {

                text =
                        text.substring(
                                0,
                                MAX_TEXT_LENGTH
                        );
            }

            return text;

        } catch (ResponseStatusException ex) {

            throw ex;

        } catch (IOException ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unable to read resume PDF"
            );
        }
    }

    private void validateFile(
            MultipartFile file) {

        if (file == null ||
                file.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume PDF is required"
            );
        }

        if (file.getSize() >
                MAX_FILE_SIZE) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Resume PDF size must not exceed 5 MB"
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null ||
                !fileName
                        .toLowerCase(Locale.ROOT)
                        .endsWith(".pdf")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF resume files are allowed"
            );
        }

        String contentType =
                file.getContentType();

        if (contentType != null &&
                !contentType.equalsIgnoreCase(
                        "application/pdf"
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only PDF resume files are allowed"
            );
        }
    }
}