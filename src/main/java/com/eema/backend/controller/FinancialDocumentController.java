package com.eema.backend.controller;

import com.eema.backend.entity.User;
import com.eema.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class FinancialDocumentController {
    private final FinancialDocumentService documents;
    private final CurrentUserService currentUserService;

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('SCOUT','ADMIN')")
    public ResponseEntity<byte[]> receipt(@PathVariable UUID id) {
        User actor = currentUserService.getCurrentUser();
        return pdf(documents.bookingReceipt(id, actor));
    }
    static ResponseEntity<byte[]> pdf(FinancialDocumentService.DocumentDownload document) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.filename()).build().toString())
                .contentLength(document.content().length).body(document.content());
    }
}
