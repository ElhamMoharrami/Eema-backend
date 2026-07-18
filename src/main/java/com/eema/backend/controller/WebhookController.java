package com.eema.backend.controller;

import com.eema.backend.service.WebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling Mollie webhooks.
 */
@Slf4j
@RestController
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Endpoint for Mollie webhooks.
     *
     * @param signature the value of the X-Mollie-Signature header
     * @param rawBody   the raw request body
     * @return 200 if processed successfully, 401 if signature invalid
     */
    @PostMapping("/api/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader("X-Mollie-Signature") String signature,
            @RequestBody String rawBody) {
        try {
            webhookService.handleWebhook(rawBody, signature);
            return ResponseEntity.ok().build();
        } catch (WebhookService.InvalidSignatureException e) {
            // Log already done in service
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            // Log and return 500 so Mollie retries
            log.error("Unexpected error processing webhook", e);
            return ResponseEntity.status(500).build();
        }
    }
}