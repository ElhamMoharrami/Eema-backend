package com.eema.backend.service;

public interface PrivateDocumentStorage {
    String store(String reference, byte[] content);
    byte[] load(String reference);
    void delete(String reference);
}
