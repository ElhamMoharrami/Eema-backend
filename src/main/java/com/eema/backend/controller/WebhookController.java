package com.eema.backend.controller;

import com.eema.backend.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for handling Mollie webhooks.
 */
@Slf4j
@RestController
@RequestMapping("/api/webhooks/mollie")
public class WebhookController {

    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    /**
     * Endpoint for Mollie to send payment webhooks.
     *
     * @param rawBody   the raw request body (as string)
     * @param signature the value of the X-Mollie-Signature header
     * @return 200 OK if processed, 200 also for invalid signature or unexpected errors (to prevent Mollie retries)
     */
    @PostMapping
    public ResponseEntity<Void> handleMollieWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Mollie-Signature") String signature) {
        try {
            webhookService.handleWebhook(rawBody, signature);
            return ResponseEntity.ok().build();
        } catch (WebhookService.InvalidSignatureException e) {
            // Log already done in service; return 200 to prevent Mollie retries for invalid signature
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Log unexpected errors but return 200 to prevent Mollie retries (Mollie treats non-2xx as failure)
            // Note: WebhookService already logs errors internally
            log.error("Unexpected error processing webhook", e);
            return ResponseEntity.ok().build();
        }
    }
}