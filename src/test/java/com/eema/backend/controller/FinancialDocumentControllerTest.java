package com.eema.backend.controller;

import com.eema.backend.entity.User;
import com.eema.backend.entity.enums.UserRole;
import com.eema.backend.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FinancialDocumentControllerTest {
    @Test void authorizedEndpointReturnsPdfAttachmentWithoutStoragePath() {
        FinancialDocumentService documents = mock(FinancialDocumentService.class);
        CurrentUserService current = mock(CurrentUserService.class);
        FinancialDocumentController controller = new FinancialDocumentController(documents, current);
        UUID id = UUID.randomUUID(); User scout = User.builder().id(UUID.randomUUID()).role(UserRole.SCOUT).build();
        when(current.getCurrentUser()).thenReturn(scout);
        when(documents.bookingReceipt(id, scout)).thenReturn(new FinancialDocumentService.DocumentDownload(
                "%PDF".getBytes(), "RCT-2026-000001", "eema-booking-receipt-RCT-2026-000001.pdf"));
        ResponseEntity<byte[]> response = controller.receipt(id);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .isEqualTo("attachment; filename=\"eema-booking-receipt-RCT-2026-000001.pdf\"")
                .doesNotContain("/tmp", "booking-receipts/");
        assertThat(response.getBody()).startsWith("%PDF".getBytes());
    }
}
