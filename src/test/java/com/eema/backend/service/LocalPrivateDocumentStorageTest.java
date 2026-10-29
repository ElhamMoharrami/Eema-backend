package com.eema.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class LocalPrivateDocumentStorageTest {
    @TempDir Path directory;
    @Test void storesOpaqueRelativeReferencesAndRejectsTraversal() {
        LocalPrivateDocumentStorage storage = new LocalPrivateDocumentStorage(directory.toString());
        byte[] pdf = "%PDF-private".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertThat(storage.store("booking-receipts/id.pdf", pdf)).isEqualTo("booking-receipts/id.pdf");
        assertThat(storage.load("booking-receipts/id.pdf")).isEqualTo(pdf);
        assertThatThrownBy(() -> storage.load("../uploads/public.pdf")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.load(directory.resolve("absolute.pdf").toString())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void publicUploadControllerHasNoPrivateDocumentStorageDependency() {
        assertThat(java.util.Arrays.stream(com.eema.backend.controller.UploadController.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getType)).doesNotContain(PrivateDocumentStorage.class);
    }
}
