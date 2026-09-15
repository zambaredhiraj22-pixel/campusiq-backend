package com.campusiq.service;

import org.springframework.web.multipart.MultipartFile;

public interface ResumePdfService {

    String extractText(MultipartFile file);
}