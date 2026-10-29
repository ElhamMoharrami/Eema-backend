package com.eema.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3PrivateDocumentStorage implements PrivateDocumentStorage {
    private final S3Client client;
    private final String bucket;
    private final String prefix;

    public S3PrivateDocumentStorage(S3Client client,
                                    @Value("${app.storage.s3.private-documents-bucket}") String bucket,
                                    @Value("${app.storage.s3.private-documents-prefix:financial-documents/}") String prefix) {
        this.client = client;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
    }

    @Override public String store(String reference, byte[] content) {
        String key = key(reference);
        client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType("application/pdf").ifNoneMatch("*").build(),
                RequestBody.fromBytes(content));
        return reference;
    }
    @Override public byte[] load(String reference) {
        return client.getObjectAsBytes(GetObjectRequest.builder().bucket(bucket).key(key(reference)).build()).asByteArray();
    }
    @Override public void delete(String reference) {
        client.deleteObject(request -> request.bucket(bucket).key(key(reference)));
    }
    private String key(String reference) {
        if (reference == null || !reference.matches("[A-Za-z0-9][A-Za-z0-9/_-]*\\.pdf") || reference.contains(".."))
            throw new IllegalArgumentException("Invalid document reference");
        return prefix + reference;
    }
    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank()) return "";
        if (value.startsWith("/") || value.contains("..")) throw new IllegalArgumentException("Invalid S3 document prefix");
        return value.endsWith("/") ? value : value + "/";
    }
}
