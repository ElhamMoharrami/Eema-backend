package com.eema.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String store(MultipartFile file);
    Resource loadAsResource(String filename);
    void delete(String storageReference);
}
